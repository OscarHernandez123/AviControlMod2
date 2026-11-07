package com.avicontrol.application.alimentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.inventario.ConsultarCoberturaAlimentoUseCase;
import com.avicontrol.application.inventario.ConsultarInventarioUseCase;
import com.avicontrol.application.nutricion.GestionarPlantillasUseCase;
import com.avicontrol.application.nutricion.PlanNutricionalLoteUseCase;
import com.avicontrol.application.recepcion.RegistrarRecepcionAlimentoCommand;
import com.avicontrol.application.recepcion.RegistrarRecepcionAlimentoUseCase;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.PlanNutricionalPlantilla;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AviControlModulo2Application.class)
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class NutricionInventarioIntegrationTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:nutricion_inventario_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  private static final UUID ALIMENTO = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired RegistrarRecepcionAlimentoUseCase recepciones;
  @Autowired GestionarPlantillasUseCase plantillas;
  @Autowired PlanNutricionalLoteUseCase planes;
  @Autowired ProyeccionAlimentoUseCase proyecciones;
  @Autowired ConsultarInventarioUseCase inventario;
  @Autowired ConsultarCoberturaAlimentoUseCase cobertura;
  @Autowired MockMvc mvc;

  @Test
  void recepcionPlanProrrogaYCoberturaMantienenCostoCapturado() throws Exception {
    LocalDate hoy = LocalDate.now(clock);
    jdbc.update(
        "INSERT INTO alimentos_comerciales_nutricion (id,nombre,tipo_alimento,peso_nominal_bulto_kg,proteina_porcentaje,energia_kcal_kg,activo) VALUES (?,?,?,?,?,?,TRUE)",
        ALIMENTO,
        "Alimento Inicio",
        "INICIO",
        new BigDecimal("40"),
        new BigDecimal("20"),
        new BigDecimal("3100"));
    var primeraRecepcion =
        recepciones.registrar(
            new RegistrarRecepcionAlimentoCommand(
                ALIMENTO,
                EtapaCrianza.INICIO,
                "NUT-001",
                hoy,
                hoy.plusDays(30),
                10,
                new BigDecimal("40"),
                new BigDecimal("400"),
                BigDecimal.ZERO,
                "admin"));
    recepciones.registrar(
        new RegistrarRecepcionAlimentoCommand(
            ALIMENTO,
            EtapaCrianza.INICIO,
            "NUT-002",
            hoy,
            hoy.plusDays(20),
            5,
            new BigDecimal("20"),
            new BigDecimal("400"),
            BigDecimal.ZERO,
            "admin"));

    var plantilla =
        plantillas.crear(
            new PlanNutricionalPlantilla(
                null,
                "Broiler",
                "BROILER",
                true,
                true,
                Instant.now(clock),
                List.of(
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.PRE_INICIO,
                        1,
                        7,
                        new BigDecimal("0.0500"),
                        EtapaCrianza.PRE_INICIO,
                        new BigDecimal("40"),
                        null),
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.INICIO,
                        8,
                        21,
                        new BigDecimal("0.1000"),
                        EtapaCrianza.INICIO,
                        new BigDecimal("40"),
                        ALIMENTO),
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.ENGORDE,
                        22,
                        45,
                        new BigDecimal("0.1200"),
                        EtapaCrianza.ENGORDE,
                        new BigDecimal("40"),
                        null))));
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    var plan =
        planes.asignarManual(
            galponId,
            new PlanNutricionalLoteUseCase.Alojamiento(
                galponId, loteId, "LDP-005", "BROILER", hoy, 8, 100),
            plantilla.id());

    assertEquals("ACTIVA", plan.etapas().get(1).estado());
    assertEquals("LDP-005", proyecciones.consultarLote(loteId).codigoLote());
    var inicial = proyecciones.consultarLote(loteId).etapas().get(0);
    assertEquals(0, new BigDecimal("140.0000").compareTo(inicial.proyeccionKg()));
    assertEquals(0, new BigDecimal("12.00").compareTo(inicial.costoUnitarioKg()));
    assertEquals(1, inventario.consultar().alimentos().size());
    assertEquals(2, inventario.consultar().alimentos().get(0).recepcionesActivas());
    assertEquals(
        0,
        new BigDecimal("500").compareTo(inventario.consultar().alimentos().get(0).stockActualKg()));
    assertEquals(hoy.plusDays(20), inventario.consultar().alimentos().get(0).proximoVencimiento());
    assertEquals("CUMPLE", cobertura.consultar(ALIMENTO).estado());
    assertEquals(0, new BigDecimal("260").compareTo(proyecciones.balance().get(0).balanceKg()));
    mvc.perform(
            get("/api/inventario")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.alimentos[0].estadoCobertura").value("CUMPLE"));
    mvc.perform(
            get("/api/inventario/resumen")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recepcionesAlimentoRecientes").isArray());
    mvc.perform(
            get("/api/inventario/alimentos/{id}/requerimiento", ALIMENTO)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CUMPLE"));
    mvc.perform(
            get("/api/galpones/{id}/plan-nutricional", galponId)
                .with(
                    SecurityMockMvcRequestPostProcessors.user("nutricionista")
                        .roles("NUTRICIONISTA")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.etapas[1].estadoEtapa").value("ACTIVA"));
    mvc.perform(
            get("/api/lotes/{id}/alimento-requerido", loteId)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigoLote").value("LDP-005"));
    mvc.perform(
            get("/api/administracion/balance-abastecimiento")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.alimentos[0].balanceKg").value(260));

    planes.ajustarDuracion(
        galponId,
        plan.etapas().get(1).id(),
        23,
        "BAJO_PESO",
        "Retraso zootécnico",
        "nutricionista");
    var ajustada = proyecciones.consultarLote(loteId).etapas().get(0);
    assertEquals(0, new BigDecimal("160.0000").compareTo(ajustada.proyeccionKg()));
    assertEquals(0, new BigDecimal("12.00").compareTo(ajustada.costoUnitarioKg()));
    assertEquals(0, new BigDecimal("240").compareTo(proyecciones.balance().get(0).balanceKg()));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM ajustes_proyeccion_alimento WHERE proyeccion_id=?",
            Integer.class,
            ajustada.id()));
    assertNotNull(planes.consultar(galponId).fechaSalidaProyectada());

    var alternativa =
        plantillas.crear(
            new PlanNutricionalPlantilla(
                null,
                "Broiler extendido",
                "BROILER",
                false,
                true,
                Instant.now(clock),
                List.of(
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.PRE_INICIO,
                        1,
                        7,
                        new BigDecimal("0.0500"),
                        EtapaCrianza.PRE_INICIO,
                        new BigDecimal("40"),
                        null),
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.INICIO,
                        8,
                        21,
                        new BigDecimal("0.1500"),
                        EtapaCrianza.INICIO,
                        new BigDecimal("40"),
                        ALIMENTO),
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.ENGORDE,
                        22,
                        50,
                        new BigDecimal("0.1200"),
                        EtapaCrianza.ENGORDE,
                        new BigDecimal("40"),
                        null))));
    var sustituido =
        planes.sustituir(galponId, alternativa.id(), "Extender etapa futura", "nutricionista");
    assertEquals(plan.etapas().get(1).id(), sustituido.etapas().get(1).id());
    assertEquals(
        0, new BigDecimal("0.1000").compareTo(sustituido.etapas().get(1).racionKgAveDia()));
    assertEquals(24, sustituido.etapas().get(2).diaInicio());
    assertEquals(52, sustituido.etapas().get(2).diaFin());
    assertEquals(
        0,
        new BigDecimal("12.00")
            .compareTo(proyecciones.consultarLote(loteId).etapas().get(0).costoUnitarioKg()));

    UUID engordeId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO alimentos_comerciales_nutricion (id,nombre,tipo_alimento,peso_nominal_bulto_kg,proteina_porcentaje,energia_kcal_kg,duracion_recomendada_etapa_dias,activo) VALUES (?,?,?,?,?,?,?,TRUE)",
        engordeId,
        "Alimento Engorde",
        "ENGORDE",
        new BigDecimal("40"),
        new BigDecimal("18"),
        new BigDecimal("3200"),
        20);
    var seleccionado =
        planes.seleccionarAlimento(
            galponId, sustituido.etapas().get(2).id(), engordeId, "nutricionista");
    assertEquals(engordeId, seleccionado.etapas().get(2).alimentoId());
    assertEquals(43, seleccionado.etapas().get(2).diaFin());
    assertEquals("SIN_STOCK", planes.compatibles(EtapaCrianza.ENGORDE).get(0).estadoStock());

    for (int i = 0; i < 2; i++) {
      jdbc.update(
          "INSERT INTO movimientos_inventario (id,recepcion_id,producto_id,tipo_producto,tipo_movimiento,cantidad,unidad,actor,fecha_registro,estado) VALUES (?,?,?,?,?,?,?,?,?,?)",
          UUID.randomUUID(),
          primeraRecepcion.id(),
          ALIMENTO,
          "ALIMENTO",
          "SALIDA",
          i == 0 ? new BigDecimal("10") : new BigDecimal("15"),
          "KILOGRAMO",
          "trabajador",
          java.sql.Timestamp.from(Instant.now(clock)),
          "CONFIRMADO");
    }
    assertEquals(
        0,
        new BigDecimal("475").compareTo(inventario.consultar().alimentos().get(0).stockActualKg()));
    assertEquals(
        0,
        new BigDecimal("12.00")
            .compareTo(proyecciones.consultarLote(loteId).etapas().get(0).costoUnitarioKg()));
  }

  @Test
  void costoAusenteSeCompletaUnaVezMientrasEtapaEstaActiva() {
    LocalDate hoy = LocalDate.now(clock);
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    var alojamiento =
        new PlanNutricionalLoteUseCase.Alojamiento(
            galpon, lote, "L-SIN-STOCK", "BROILER", hoy, 8, 100);
    assertEquals("PENDIENTE_ASIGNACION", planes.asignarAlAlojar(alojamiento).estado());
    jdbc.update(
        "INSERT INTO alimentos_comerciales_nutricion (id,nombre,tipo_alimento,peso_nominal_bulto_kg,proteina_porcentaje,energia_kcal_kg,activo) VALUES (?,?,?,?,?,?,TRUE)",
        ALIMENTO,
        "Alimento Inicio",
        "INICIO",
        new BigDecimal("40"),
        new BigDecimal("20"),
        new BigDecimal("3100"));
    var plantilla =
        plantillas.crear(
            new PlanNutricionalPlantilla(
                null,
                "Sin stock",
                "BROILER",
                true,
                true,
                Instant.now(clock),
                List.of(
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.PRE_INICIO,
                        1,
                        7,
                        new BigDecimal("0.0500"),
                        EtapaCrianza.PRE_INICIO,
                        new BigDecimal("40"),
                        null),
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.INICIO,
                        8,
                        21,
                        new BigDecimal("0.1000"),
                        EtapaCrianza.INICIO,
                        new BigDecimal("40"),
                        ALIMENTO),
                    new PlanNutricionalPlantilla.Etapa(
                        EtapaCrianza.ENGORDE,
                        22,
                        45,
                        new BigDecimal("0.1200"),
                        EtapaCrianza.ENGORDE,
                        new BigDecimal("40"),
                        null))));
    planes.asignarManual(galpon, alojamiento, plantilla.id());
    var proyeccion = proyecciones.consultarLote(lote).etapas().get(0);
    assertNull(proyeccion.costoUnitarioKg());
    assertNotNull(proyeccion.advertenciaCosto());
    proyecciones.completarCosto(
        proyeccion.id(), new BigDecimal("12.34"), "Precio histórico comprobado", "admin");
    assertThrows(
        IllegalStateException.class,
        () ->
            proyecciones.completarCosto(
                proyeccion.id(), new BigDecimal("12.34"), "Segundo intento", "admin"));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM auditoria_costo_proyeccion WHERE proyeccion_id=?",
            Integer.class,
            proyeccion.id()));
  }
}
