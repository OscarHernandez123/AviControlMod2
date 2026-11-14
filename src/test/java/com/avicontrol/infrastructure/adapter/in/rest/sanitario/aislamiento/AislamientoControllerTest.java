package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.sanitario.aislamiento.ValidarAislamientoCommand;
import com.avicontrol.application.sanitario.aislamiento.ValidarAislamientoUseCase;
import com.avicontrol.application.sanitario.aislamiento.result.ValidacionAislamientoResult;
import com.avicontrol.domain.exception.sanitario.aislamiento.EstadoGalponNoAislableException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoNoPendienteException;
import com.avicontrol.domain.model.sanitario.aislamiento.DecisionAislamiento;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
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
class AislamientoControllerTest {
  private static final UUID GALPON = UUID.fromString("550e8400-e29b-41d4-a716-446655440101");
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ValidarAislamientoUseCase validar;
  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM solicitudes_aislamiento WHERE galpon_id = ?", GALPON);
    jdbc.update(
        "UPDATE modulo1_galpon_local SET estado = 'PRODUCTIVA', poblacion_viva = 8000 WHERE id = ?",
        GALPON);
  }

  @Test
  void aceptaUnaVezYActualizaSoloEstado() throws Exception {
    UUID solicitud = crearSolicitud();
    mvc.perform(
            post("/api/solicitudes-aislamiento/{id}/validacion", solicitud)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"decision":"ACEPTAR","observaciones":"Aislamiento requerido"}
            """)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ACEPTADA"))
        .andExpect(jsonPath("$.estadoGalponResultante").value("AISLAMIENTO"));
    assertEquals(
        "AISLAMIENTO",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id = ?", String.class, GALPON));
    assertEquals(
        8000,
        jdbc.queryForObject(
            "SELECT poblacion_viva FROM modulo1_galpon_local WHERE id = ?", Integer.class, GALPON));
    mvc.perform(
            get("/api/solicitudes-aislamiento/{id}", solicitud)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.observaciones").value("Aislamiento requerido"))
        .andExpect(jsonPath("$.veterinario").value("vet"));
    mvc.perform(
            post("/api/solicitudes-aislamiento/{id}/validacion", solicitud)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"decision":"ACEPTAR","observaciones":"Repetir"}
            """)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isConflict());
  }

  @Test
  void rechazaSinAislarYCompruebaEstadoVigente() throws Exception {
    UUID solicitud = crearSolicitud();
    mvc.perform(
            post("/api/solicitudes-aislamiento/{id}/validacion", solicitud)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"decision":"RECHAZAR","observaciones":"No procede"}
            """)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("RECHAZADA"));
    assertEquals(
        "PRODUCTIVA",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id = ?", String.class, GALPON));
    UUID nueva = crearSolicitud();
    jdbc.update("UPDATE modulo1_galpon_local SET estado = 'MANTENIMIENTO' WHERE id = ?", GALPON);
    mvc.perform(
            post("/api/solicitudes-aislamiento/{id}/validacion", nueva)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"decision":"ACEPTAR","observaciones":"Aislar"}
            """)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isConflict());
    assertEquals(
        "PENDIENTE_VALIDACION",
        jdbc.queryForObject(
            "SELECT estado FROM solicitudes_aislamiento WHERE id = ?", String.class, nueva));
  }

  @Test
  void exigeVeterinarioYSolicitudPrevia() throws Exception {
    UUID solicitud = crearSolicitud();
    mvc.perform(
            post("/api/solicitudes-aislamiento/{id}/validacion", solicitud)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"decision":"ACEPTAR","observaciones":"Aislar"}
            """)
                .with(SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/solicitudes-aislamiento/{id}/validacion", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"decision":"ACEPTAR","observaciones":"Aislar"}
            """)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isNotFound());
  }

  @Test
  void dosValidacionesSimultaneasAceptanUnaSolaDecision() throws Exception {
    UUID solicitud = crearSolicitud();
    CountDownLatch inicio = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Object> tarea =
          () -> {
            inicio.await();
            try {
              return validar.validar(
                  new ValidarAislamientoCommand(
                      solicitud, DecisionAislamiento.ACEPTAR, "Aislar", "vet"));
            } catch (RuntimeException e) {
              return e;
            }
          };
      Future<Object> primera = executor.submit(tarea);
      Future<Object> segunda = executor.submit(tarea);
      inicio.countDown();
      Object a = primera.get(15, TimeUnit.SECONDS);
      Object b = segunda.get(15, TimeUnit.SECONDS);
      assertTrue(
          (a instanceof ValidacionAislamientoResult
                  && (b instanceof EstadoGalponNoAislableException
                      || b instanceof SolicitudAislamientoNoPendienteException))
              || (b instanceof ValidacionAislamientoResult
                  && (a instanceof EstadoGalponNoAislableException
                      || a instanceof SolicitudAislamientoNoPendienteException)));
    }
    assertEquals(
        "AISLAMIENTO",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id = ?", String.class, GALPON));
    assertEquals(
        "ACEPTADA",
        jdbc.queryForObject(
            "SELECT estado FROM solicitudes_aislamiento WHERE id = ?", String.class, solicitud));
  }

  private UUID crearSolicitud() throws Exception {
    String response =
        mvc.perform(
                post("/api/galpones/{id}/solicitudes-aislamiento", GALPON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
            {"motivo":"Signos respiratorios","signosClinicos":"Tos y decaimiento"}
            """)
                    .with(
                        SecurityMockMvcRequestPostProcessors.user("trabajador")
                            .roles("TRABAJADOR")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(response).get("id").asText());
  }
}
