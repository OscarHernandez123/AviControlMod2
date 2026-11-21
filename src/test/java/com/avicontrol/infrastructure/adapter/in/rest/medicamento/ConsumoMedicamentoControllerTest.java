package com.avicontrol.infrastructure.adapter.in.rest.medicamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.application.medicamento.RegistrarConsumoMedicamentoCommand;
import com.avicontrol.application.medicamento.RegistrarConsumoMedicamentoUseCase;
import com.avicontrol.application.medicamento.result.ConsumoMedicamentoResult;
import com.avicontrol.domain.exception.medicamento.ExistenciasMedicamentoInsuficientesException;
import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.inventario.InventarioMovimientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsumoMedicamentoControllerTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired DiagnosticoGalponRepositoryPort diagnosticos;
  @Autowired InventarioMovimientoQueryPort inventario;
  @Autowired AjustarEntradaInventarioPort ajusteEntradas;
  @Autowired RegistrarConsumoMedicamentoUseCase useCase;
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void descuentaFefoEnDosRecepcionesConPreciosHistoricosEIdempotencia() throws Exception {
    UUID medicamento = UUID.randomUUID();
    DiagnosticoGalpon d = diagnostico(medicamento);
    LocalDate hoy = LocalDate.now(clock);
    UUID primero =
        recepcion(
            medicamento,
            new BigDecimal("100"),
            new BigDecimal("100"),
            hoy.plusMonths(1),
            "MILILITRO");
    UUID segundo =
        recepcion(
            medicamento,
            new BigDecimal("500"),
            new BigDecimal("30"),
            hoy.plusMonths(2),
            "MILILITRO");
    UUID clave = UUID.randomUUID();
    String body = body("250", "MILILITRO", hoy);
    String response =
        mvc.perform(
                post("/api/diagnosticos/{id}/consumos-medicamento", d.id())
                    .header("Idempotency-Key", clave)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cantidadUnidadBase").value(250))
            .andExpect(jsonPath("$.detallesInventario.length()").value(2))
            .andExpect(jsonPath("$.detallesInventario[0].recepcionId").value(primero.toString()))
            .andExpect(jsonPath("$.detallesInventario[0].cantidadUnidadBase").value(100))
            .andExpect(jsonPath("$.detallesInventario[0].precioNetoHistoricoUnidadBase").value(100))
            .andExpect(jsonPath("$.detallesInventario[1].recepcionId").value(segundo.toString()))
            .andExpect(jsonPath("$.detallesInventario[1].cantidadUnidadBase").value(150))
            .andExpect(jsonPath("$.detallesInventario[1].precioNetoHistoricoUnidadBase").value(30))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID consumo = UUID.fromString(json.readTree(response).get("id").asText());
    assertEquals(0, new BigDecimal("350").compareTo(stock(medicamento)));
    assertEquals(true, ajusteEntradas.tieneMovimientosPosteriores(primero));
    String replay =
        mvc.perform(
                post("/api/diagnosticos/{id}/consumos-medicamento", d.id())
                    .header("Idempotency-Key", clave)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(consumo.toString(), json.readTree(replay).get("id").asText());
    assertEquals(0, new BigDecimal("350").compareTo(stock(medicamento)));
    mvc.perform(
            get("/api/consumos-medicamento/{id}", consumo)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.detallesInventario.length()").value(2));
    mvc.perform(
            get("/api/lotes/{id}/consumos-medicamento", d.loteId())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(consumo.toString()))
        .andExpect(jsonPath("$[0].detallesInventario.length()").value(2));
    mvc.perform(
            post("/api/diagnosticos/{id}/consumos-medicamento", d.id())
                .header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("200", "MILILITRO", hoy))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isConflict());
  }

  @Test
  void noDescuentaCuandoFaltanExistencias() throws Exception {
    UUID medicamento = UUID.randomUUID();
    DiagnosticoGalpon d = diagnostico(medicamento);
    LocalDate hoy = LocalDate.now(clock);
    recepcion(
        medicamento, new BigDecimal("100"), new BigDecimal("10"), hoy.plusMonths(1), "MILILITRO");
    mvc.perform(
            post("/api/diagnosticos/{id}/consumos-medicamento", d.id())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("101", "MILILITRO", hoy))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().is(422));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM consumos_medicamento WHERE diagnostico_id = ?",
            Integer.class,
            d.id()));
    assertEquals(0, new BigDecimal("100").compareTo(stock(medicamento)));
  }

  @Test
  void validaUnidadFechaYRol() throws Exception {
    UUID medicamento = UUID.randomUUID();
    DiagnosticoGalpon d = diagnostico(medicamento);
    LocalDate hoy = LocalDate.now(clock);
    recepcion(
        medicamento, new BigDecimal("100"), new BigDecimal("10"), hoy.plusMonths(1), "MILILITRO");
    mvc.perform(
            post("/api/diagnosticos/{id}/consumos-medicamento", d.id())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("1", "UNIDAD", hoy))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/diagnosticos/{id}/consumos-medicamento", d.id())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("1", "MILILITRO", hoy.plusDays(1)))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/diagnosticos/{id}/consumos-medicamento", d.id())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("1", "MILILITRO", hoy))
                .with(SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
    assertEquals(0, new BigDecimal("100").compareTo(stock(medicamento)));
  }

  @Test
  void convierteMiligramosAGramos() throws Exception {
    UUID medicamento = UUID.randomUUID();
    DiagnosticoGalpon d = diagnostico(medicamento);
    LocalDate hoy = LocalDate.now(clock);
    recepcion(medicamento, new BigDecimal("1"), new BigDecimal("20"), hoy.plusMonths(1), "GRAMO");
    mvc.perform(
            post("/api/diagnosticos/{id}/consumos-medicamento", d.id())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("250", "MILIGRAMO", hoy))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.cantidadUnidadBase").value(0.25))
        .andExpect(jsonPath("$.unidadBase").value("GRAMO"));
    assertEquals(0, new BigDecimal("0.75").compareTo(stock(medicamento)));
  }

  @Test
  void dosConsumosSimultaneosNoSobreDescuentan() throws Exception {
    UUID medicamento = UUID.randomUUID();
    DiagnosticoGalpon d = diagnostico(medicamento);
    LocalDate hoy = LocalDate.now(clock);
    recepcion(
        medicamento, new BigDecimal("100"), new BigDecimal("10"), hoy.plusMonths(1), "MILILITRO");
    CountDownLatch inicio = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Object> tarea =
          () -> {
            inicio.await();
            try {
              return useCase.registrar(
                  new RegistrarConsumoMedicamentoCommand(
                      d.id(),
                      new BigDecimal("75"),
                      UnidadConsumoMedicamento.MILILITRO,
                      hoy,
                      "vet",
                      UUID.randomUUID()));
            } catch (RuntimeException e) {
              return e;
            }
          };
      Future<Object> primero = executor.submit(tarea);
      Future<Object> segundo = executor.submit(tarea);
      inicio.countDown();
      Object a = primero.get(15, TimeUnit.SECONDS);
      Object b = segundo.get(15, TimeUnit.SECONDS);
      assertTrue(
          (a instanceof ConsumoMedicamentoResult
                  && b instanceof ExistenciasMedicamentoInsuficientesException)
              || (b instanceof ConsumoMedicamentoResult
                  && a instanceof ExistenciasMedicamentoInsuficientesException));
    }
    assertEquals(0, new BigDecimal("25").compareTo(stock(medicamento)));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM consumos_medicamento WHERE diagnostico_id = ?",
            Integer.class,
            d.id()));
  }

  @Test
  void dosReintentosSimultaneosConMismaClaveDevuelvenElMismoConsumo() throws Exception {
    UUID medicamento = UUID.randomUUID();
    DiagnosticoGalpon d = diagnostico(medicamento);
    LocalDate hoy = LocalDate.now(clock);
    recepcion(
        medicamento, new BigDecimal("100"), new BigDecimal("10"), hoy.plusMonths(1), "MILILITRO");
    UUID clave = UUID.randomUUID();
    CountDownLatch inicio = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<ConsumoMedicamentoResult> tarea =
          () -> {
            inicio.await();
            return useCase.registrar(
                new RegistrarConsumoMedicamentoCommand(
                    d.id(),
                    new BigDecimal("75"),
                    UnidadConsumoMedicamento.MILILITRO,
                    hoy,
                    "vet",
                    clave));
          };
      Future<ConsumoMedicamentoResult> primero = executor.submit(tarea);
      Future<ConsumoMedicamentoResult> segundo = executor.submit(tarea);
      inicio.countDown();
      assertEquals(primero.get(15, TimeUnit.SECONDS).id(), segundo.get(15, TimeUnit.SECONDS).id());
    }
    assertEquals(0, new BigDecimal("25").compareTo(stock(medicamento)));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM consumos_medicamento WHERE diagnostico_id = ?",
            Integer.class,
            d.id()));
  }

  private DiagnosticoGalpon diagnostico(UUID medicamento) {
    Instant now = Instant.now(clock);
    LocalDate hoy = LocalDate.now(clock);
    Enfermedad e =
        new Enfermedad(
            UUID.randomUUID(),
            "Tratamiento",
            NivelRiesgo.BAJO,
            TipoEnfermedad.BACTERIANA,
            "Signos",
            false,
            0,
            now,
            now);
    Medicacion m =
        new Medicacion(UUID.randomUUID(), e.id(), medicamento, "1 ml", 5, "Aplicar", 0, now, now);
    jdbc.update(
        "INSERT INTO enfermedades (id, nombre, nivel_riesgo, tipo, descripcion_clinica, requiere_sacrificio_sanitario, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        e.id(),
        e.nombre(),
        e.nivelRiesgo().name(),
        e.tipo().name(),
        e.descripcionClinica(),
        false,
        0,
        Timestamp.from(now),
        Timestamp.from(now));
    jdbc.update(
        "INSERT INTO medicaciones (id, enfermedad_id, medicamento_id, dosis, duracion_dias, descripcion, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        m.id(),
        e.id(),
        medicamento,
        m.dosis(),
        m.duracionDias(),
        m.descripcion(),
        0,
        Timestamp.from(now),
        Timestamp.from(now));
    DiagnosticoGalpon d =
        DiagnosticoGalpon.crear(UUID.randomUUID(), UUID.randomUUID(), e, m, "Tratamiento", hoy);
    diagnosticos.crear(d);
    return d;
  }

  private UUID recepcion(
      UUID medicamento,
      BigDecimal cantidad,
      BigDecimal precioBase,
      LocalDate vencimiento,
      String unidad) {
    UUID id = UUID.randomUUID();
    LocalDate hoy = LocalDate.now(clock);
    BigDecimal total = cantidad.multiply(precioBase);
    jdbc.update(
        "INSERT INTO recepciones_medicamento (id, medicamento_id, codigo_lote, presentacion, fecha_ingreso, fecha_vencimiento, cantidad_presentaciones, contenido_por_presentacion, unidad_contenido, precio_neto_por_presentacion, impuesto_porcentaje, cantidad_unidad_base, unidad_base, precio_neto_unidad_base, precio_neto_total, valor_impuesto, precio_total, estado, actor, nombre_medicamento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        medicamento,
        "LOTE-" + id,
        "Frasco",
        hoy.minusDays(1),
        vencimiento,
        1,
        cantidad,
        unidad,
        total,
        BigDecimal.ZERO,
        cantidad,
        unidad,
        precioBase,
        total,
        BigDecimal.ZERO,
        total,
        "CONFIRMADA",
        "admin",
        "Medicamento");
    jdbc.update(
        "INSERT INTO movimientos_inventario (id, recepcion_id, producto_id, tipo_producto, tipo_movimiento, cantidad, unidad, actor, fecha_registro, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        id,
        medicamento,
        "MEDICAMENTO",
        "ENTRADA",
        cantidad,
        unidad,
        "admin",
        Timestamp.from(Instant.now(clock)),
        "CONFIRMADO");
    return id;
  }

  private BigDecimal stock(UUID medicamento) {
    return inventario.consultar(LocalDate.now(clock)).medicamentos().stream()
        .filter(m -> medicamento.equals(m.medicamentoId()))
        .map(m -> m.stockActual())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private String body(String cantidad, String unidad, LocalDate fecha) {
    return """
        {"cantidad":%s,"unidad":"%s","fechaAplicacion":"%s"}
        """
        .formatted(cantidad, unidad, fecha);
  }
}
