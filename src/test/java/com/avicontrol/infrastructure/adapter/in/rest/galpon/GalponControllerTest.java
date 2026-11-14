package com.avicontrol.infrastructure.adapter.in.rest.galpon;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GalponControllerTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:galpon_consulta_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM modulo1_galpon_local");
  }

  @Test
  void detalleConLoteSinLoteYFechaFutura() throws Exception {
    UUID conLote = crear("PRODUCTIVA", "G1", UUID.randomUUID(), LocalDate.now(clock).minusDays(16));
    UUID sinLote = crear("DISPONIBLE", "G2", null, null);
    UUID futuro = crear("AISLAMIENTO", "G3", UUID.randomUUID(), LocalDate.now(clock).plusDays(1));
    mvc.perform(get("/api/galpones/{id}", conLote).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("G1"))
        .andExpect(jsonPath("$.estado").value("PRODUCTIVO"))
        .andExpect(jsonPath("$.loteActual.poblacionActual").value(8000))
        .andExpect(jsonPath("$.loteActual.edad.diasTotales").value(17));
    mvc.perform(get("/api/galpones/{id}", sinLote).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loteActual").isEmpty())
        .andExpect(jsonPath("$.mensaje").value("El galpón no tiene lote alojado actualmente"));
    mvc.perform(get("/api/galpones/{id}", futuro).with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loteActual.edad").isEmpty())
        .andExpect(
            jsonPath("$.loteActual.mensaje")
                .value("La fecha de ingreso del lote es inconsistente"));
  }

  @Test
  void resumenCuentaEstadosYEtiquetaInconsistencia() throws Exception {
    crear("DISPONIBLE", "A", null, null);
    crear("VACIADO_SANITARIO", "B", null, null);
    crear("PRODUCTIVA", "C", null, null);
    crear("EN_COSECHA", "D", null, null);
    crear("MANTENIMIENTO", "E", null, null);
    crear("AISLAMIENTO", "F", null, null);
    crear("OTRO", "G", null, null);
    mvc.perform(get("/api/galpones/resumen").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalRegistrado").value(7))
        .andExpect(jsonPath("$.totalValido").value(6))
        .andExpect(jsonPath("$.registrosInconsistentes").value(1))
        .andExpect(jsonPath("$.porEstado.PRODUCTIVO").value(1))
        .andExpect(jsonPath("$.porEstado.AISLAMIENTO").value(1));
    mvc.perform(get("/api/galpones/resumen").with(trabajador())).andExpect(status().isForbidden());
  }

  @Test
  void listadoPaginadoOrdenadoYValidado() throws Exception {
    crear("DISPONIBLE", "B", null, null);
    crear("PRODUCTIVA", "A", null, null);
    mvc.perform(get("/api/galpones?page=0&size=1&sort=nombre,asc").with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.elementos[0].nombre").value("A"))
        .andExpect(jsonPath("$.totalElementos").value(2))
        .andExpect(jsonPath("$.totalPaginas").value(2));
    mvc.perform(get("/api/galpones?page=1&size=1&sort=nombre,asc").with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.elementos[0].nombre").value("B"));
    mvc.perform(get("/api/galpones?page=-1").with(trabajador())).andExpect(status().isBadRequest());
    mvc.perform(get("/api/galpones?sort=nombre;DROP,asc").with(trabajador()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void ausenciaVacioYAutorizacion() throws Exception {
    mvc.perform(get("/api/galpones/{id}", UUID.randomUUID()).with(admin()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/galpones").with(trabajador()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.elementos.length()").value(0));
    mvc.perform(get("/api/galpones")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/galpones/resumen")).andExpect(status().isUnauthorized());
  }

  private UUID crear(String estado, String nombre, UUID lote, LocalDate ingreso) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id,lote_id,estado,poblacion_viva,fecha_ingreso,nombre,aforo_maximo,lote_codigo) VALUES (?,?,?,?,?,?,?,?)",
        id,
        lote,
        estado,
        lote == null ? 0 : 8000,
        ingreso,
        nombre,
        9000,
        lote == null ? null : "LOTE-1");
    return id;
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
    return SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR");
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor trabajador() {
    return SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR");
  }
}
