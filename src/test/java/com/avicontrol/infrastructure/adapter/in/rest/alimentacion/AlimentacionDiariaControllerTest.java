package com.avicontrol.infrastructure.adapter.in.rest.alimentacion;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.alimentacion.RegistrarSuministroDiarioUseCase;
import com.avicontrol.application.alimentacion.port.ProyeccionAlimentoRepositoryPort;
import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.port.out.alimentacion.InventarioAlimentoPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlimentacionDiariaControllerTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:alimentacion_diaria_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired InventarioAlimentoPort inventario;
  @Autowired AjustarEntradaInventarioPort ajustes;
  @Autowired RegistrarSuministroDiarioUseCase registro;
  @Autowired ProyeccionAlimentoRepositoryPort proyecciones;
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void consultaYRegistraSuministroFefoConReintentoSinDobleDescuento() throws Exception {
    Escenario e = escenario(10000, new BigDecimal("0.1200"), true);
    UUID primera =
        recepcion(e.alimentoId(), new BigDecimal("600"), LocalDate.now(clock).plusDays(10));
    UUID segunda =
        recepcion(e.alimentoId(), new BigDecimal("1000"), LocalDate.now(clock).plusDays(20));
    mvc.perform(get("/api/galpones/{id}/alimento-diario", e.galponId()).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreGalpon").value("Galpón 5"))
        .andExpect(jsonPath("$.loteCodigo").value("LDP-005"))
        .andExpect(jsonPath("$.etapaCrianza").value("INICIO"))
        .andExpect(jsonPath("$.racionGramosAve").value(120.0))
        .andExpect(jsonPath("$.requerimientoKg").value(1200.0))
        .andExpect(jsonPath("$.requerimientoBultos").value(30.0))
        .andExpect(jsonPath("$.disponibilidadKg").value(1600.0))
        .andExpect(jsonPath("$.alertas[*].tipo").value(hasItem("SUMINISTRO_PENDIENTE")))
        .andExpect(jsonPath("$.alertas[*].tipo").value(hasItem("PLAN_ACTUALIZADO_HOY")));
    UUID clave = UUID.randomUUID();
    String respuesta =
        mvc.perform(
                post("/api/galpones/{id}/suministros-alimento", e.galponId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body("700", clave))
                    .with(trabajador()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cantidadKg").value(700))
            .andExpect(jsonPath("$.detalles.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID suministroId = UUID.fromString(json.readTree(respuesta).get("id").asText());
    assertEquals(0, new BigDecimal("600").compareTo(detalle(suministroId, primera)));
    assertEquals(0, new BigDecimal("100").compareTo(detalle(suministroId, segunda)));
    assertTrue(ajustes.tieneMovimientosPosteriores(primera));
    assertEquals(0, new BigDecimal("900").compareTo(stock(e.alimentoId())));
    assertEquals(
        0,
        proyecciones
            .stock(e.alimentoId(), new BigDecimal("600"), LocalDate.now(clock))
            .compareTo(BigDecimal.ZERO));
    assertEquals(
        0,
        new BigDecimal("900")
            .compareTo(
                proyecciones.stock(e.alimentoId(), new BigDecimal("1000"), LocalDate.now(clock))));
    assertEquals(
        0,
        new BigDecimal("900")
            .compareTo(
                proyecciones.saldosElegibles(e.alimentoId(), LocalDate.now(clock)).stream()
                    .map(ProyeccionAlimentoRepositoryPort.CostoSaldo::saldo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
    mvc.perform(
            post("/api/galpones/{id}/suministros-alimento", e.galponId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("700.0000", clave))
                .with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(suministroId.toString()));
    mvc.perform(
            post("/api/galpones/{id}/suministros-alimento", e.galponId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("701", clave))
                .with(trabajador()))
        .andExpect(status().isConflict());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM suministros_diarios_alimento WHERE galpon_id = ?",
            Integer.class,
            e.galponId()));
    mvc.perform(get("/api/galpones/{id}/alimento-diario", e.galponId()).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suministradoKg").value(700.0))
        .andExpect(jsonPath("$.saldoKg").value(500.0))
        .andExpect(jsonPath("$.disponibilidadKg").value(900.0));
  }

  @Test
  void insuficienciaNoPersisteNiDescuentaYAplicaSeguridad() throws Exception {
    Escenario e = escenario(10000, new BigDecimal("0.1200"), false);
    recepcion(e.alimentoId(), new BigDecimal("600"), LocalDate.now(clock).plusDays(10));
    recepcion(e.alimentoId(), new BigDecimal("500"), LocalDate.now(clock).minusDays(1));
    mvc.perform(get("/api/galpones/{id}/alimento-diario", e.galponId()).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.disponibilidadKg").value(600.0))
        .andExpect(jsonPath("$.alertas[0].tipo").value("STOCK_INSUFICIENTE"));
    mvc.perform(
            post("/api/galpones/{id}/suministros-alimento", e.galponId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("700", UUID.randomUUID()))
                .with(trabajador()))
        .andExpect(status().is(422));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM suministros_diarios_alimento WHERE galpon_id = ?",
            Integer.class,
            e.galponId()));
    assertEquals(0, new BigDecimal("600").compareTo(stock(e.alimentoId())));
    mvc.perform(get("/api/galpones/{id}/alimento-diario", e.galponId()))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/galpones/{id}/alimento-diario", e.galponId())
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/galpones/{id}/suministros-alimento", e.galponId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("100", UUID.randomUUID())))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/galpones/{id}/suministros-alimento", e.galponId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("100", UUID.randomUUID()))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isForbidden());
  }

  @Test
  void resumenAgrupaDemandaPorAlimentoYExcluyeGalponesNoSeleccionados() throws Exception {
    Escenario primero = escenario(10000, new BigDecimal("0.1200"), false);
    UUID segundoGalpon = UUID.randomUUID();
    UUID segundoLote = UUID.randomUUID();
    galpon(segundoGalpon, segundoLote, 10000);
    plan(segundoGalpon, segundoLote, primero.alimentoId(), new BigDecimal("0.1200"));
    recepcion(primero.alimentoId(), new BigDecimal("1600"), LocalDate.now(clock).plusDays(20));
    mvc.perform(
            get("/api/alimento-diario/resumen")
                .param("galponIds", primero.galponId().toString(), segundoGalpon.toString())
                .with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requerimientoTotalKg").value(2400.0))
        .andExpect(jsonPath("$.galpones.length()").value(2))
        .andExpect(jsonPath("$.estadoBodega").value("BODEGA_INSUFICIENTE"))
        .andExpect(jsonPath("$.galpones[0].estadoBodega").value("INSUFICIENTE"));
    mvc.perform(get("/api/alimento-diario/resumen").with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requerimientoTotalKg").value(0))
        .andExpect(jsonPath("$.galpones").isEmpty());
  }

  @Test
  void galponSinLoteYSinRangoDevuelveCuotaCero() throws Exception {
    UUID sinLote = UUID.randomUUID();
    galpon(sinLote, null, 0);
    mvc.perform(get("/api/galpones/{id}/alimento-diario", sinLote).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("SIN_LOTE_ACTIVO"))
        .andExpect(jsonPath("$.requerimientoKg").value(0));
    UUID sinAves = UUID.randomUUID();
    galpon(sinAves, UUID.randomUUID(), 0);
    mvc.perform(get("/api/galpones/{id}/alimento-diario", sinAves).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("SIN_AVES_VIVAS"))
        .andExpect(jsonPath("$.alertas").isEmpty());
    Escenario fuera = escenario(8000, new BigDecimal("0.1200"), false);
    jdbc.update(
        "UPDATE etapas_plan_nutricional SET dia_inicio = 20, dia_fin = 30 WHERE plan_id = ?",
        fuera.planId());
    mvc.perform(get("/api/galpones/{id}/alimento-diario", fuera.galponId()).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("SIN_ETAPA_NUTRICIONAL"))
        .andExpect(jsonPath("$.requerimientoKg").value(0));
    mvc.perform(get("/api/galpones/{id}/alimento-diario", UUID.randomUUID()).with(trabajador()))
        .andExpect(status().isNotFound());
  }

  @Test
  void solicitudesSimultaneasConLaMismaClaveCreanUnSoloSuministro() throws Exception {
    Escenario e = escenario(1000, new BigDecimal("0.1000"), false);
    recepcion(e.alimentoId(), new BigDecimal("100"), LocalDate.now(clock).plusDays(10));
    UUID clave = UUID.randomUUID();
    CountDownLatch inicio = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<RegistrarSuministroDiarioUseCase.Registro> tarea =
          () -> {
            inicio.await();
            return registro.registrar(
                e.galponId(), new BigDecimal("75"), LocalDate.now(clock), clave, "worker");
          };
      var primero = executor.submit(tarea);
      var segundo = executor.submit(tarea);
      inicio.countDown();
      var a = primero.get(15, TimeUnit.SECONDS);
      var b = segundo.get(15, TimeUnit.SECONDS);
      assertEquals(a.suministro().id(), b.suministro().id());
      assertTrue(a.repetido() != b.repetido());
    }
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM suministros_diarios_alimento WHERE galpon_id = ?",
            Integer.class,
            e.galponId()));
    assertEquals(0, new BigDecimal("25").compareTo(stock(e.alimentoId())));
  }

  private Escenario escenario(int poblacion, BigDecimal racion, boolean cambioHoy) {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    UUID alimento = UUID.randomUUID();
    galpon(galpon, lote, poblacion);
    jdbc.update(
        "INSERT INTO alimentos_comerciales_nutricion (id, nombre, tipo_alimento, peso_nominal_bulto_kg, proteina_porcentaje, energia_kcal_kg, activo) VALUES (?, ?, ?, ?, ?, ?, TRUE)",
        alimento,
        "Inicio Premium",
        "INICIO",
        40,
        20,
        3000);
    UUID plan = plan(galpon, lote, alimento, racion);
    if (cambioHoy)
      jdbc.update(
          "INSERT INTO auditoria_planes_nutricionales (id, plan_id, etapa_id, accion, actor, fecha_registro) VALUES (?, ?, ?, ?, ?, ?)",
          UUID.randomUUID(),
          plan,
          null,
          "AJUSTE",
          "nutri",
          Instant.now(clock));
    return new Escenario(galpon, lote, alimento, plan);
  }

  private void galpon(UUID id, UUID lote, int poblacion) {
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id, lote_id, estado, poblacion_viva, fecha_ingreso, nombre, aforo_maximo, lote_codigo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        lote,
        "PRODUCTIVA",
        poblacion,
        lote == null ? null : LocalDate.now(clock).minusDays(5),
        "Galpón 5",
        12000,
        lote == null ? null : "LDP-005");
  }

  private UUID plan(UUID galpon, UUID lote, UUID alimento, BigDecimal racion) {
    UUID plan = UUID.randomUUID();
    LocalDate ingreso = LocalDate.now(clock).minusDays(5);
    jdbc.update(
        "INSERT INTO planes_nutricionales_lote (id, galpon_id, lote_id, codigo_lote, tipo_ave, fecha_ingreso, edad_ingreso_dias, poblacion_viva, fecha_salida_proyectada, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        plan,
        galpon,
        lote,
        "LDP-005",
        "BROILER",
        ingreso,
        1,
        10000,
        ingreso.plusDays(45),
        "ACTIVO");
    jdbc.update(
        "INSERT INTO etapas_plan_nutricional (id, plan_id, etapa_crianza, estado, dia_inicio, dia_fin, racion_kg_ave_dia, tipo_alimento, alimento_id, peso_nominal_bulto_kg) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        plan,
        "INICIO",
        "ACTIVA",
        1,
        10,
        racion,
        "INICIO",
        alimento,
        40);
    return plan;
  }

  private UUID recepcion(UUID alimento, BigDecimal kg, LocalDate vencimiento) {
    UUID id = UUID.randomUUID();
    BigDecimal total = kg.multiply(new BigDecimal("10"));
    jdbc.update(
        "INSERT INTO recepciones_alimento (id, alimento_id, codigo_lote, fecha_ingreso, fecha_vencimiento, cantidad_bultos, peso_por_bulto_kg, precio_neto_por_bulto, impuesto_porcentaje, cantidad_total_kg, precio_neto_kg, precio_neto_total, valor_impuesto, precio_total, estado, actor, nombre_alimento, tipo_alimento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        alimento,
        "RX-" + id,
        LocalDate.now(clock).minusDays(10),
        vencimiento,
        1,
        kg,
        total,
        0,
        kg,
        10,
        total,
        0,
        total,
        "CONFIRMADA",
        "admin",
        "Inicio Premium",
        "INICIO");
    jdbc.update(
        "INSERT INTO movimientos_inventario (id, recepcion_id, producto_id, tipo_producto, tipo_movimiento, cantidad, unidad, actor, fecha_registro, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        id,
        alimento,
        "ALIMENTO",
        "ENTRADA",
        kg,
        "KG",
        "admin",
        Instant.now(clock),
        "CONFIRMADO");
    return id;
  }

  private BigDecimal stock(UUID alimento) {
    return inventario.disponible(alimento, "INICIO", LocalDate.now(clock));
  }

  private BigDecimal detalle(UUID suministro, UUID recepcion) {
    return jdbc.queryForObject(
        "SELECT cantidad_kg FROM detalles_suministro_alimento WHERE suministro_id = ? AND recepcion_id = ?",
        BigDecimal.class,
        suministro,
        recepcion);
  }

  private String body(String kg, UUID clave) {
    return """
        {"cantidadKg":%s,"fechaAplicacion":"%s","idempotencyKey":"%s"}
        """
        .formatted(kg, LocalDate.now(clock), clave);
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor trabajador() {
    return SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR");
  }

  private record Escenario(UUID galponId, UUID loteId, UUID alimentoId, UUID planId) {}
}
