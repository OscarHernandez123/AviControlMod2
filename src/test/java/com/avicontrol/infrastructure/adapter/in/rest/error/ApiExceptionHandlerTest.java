package com.avicontrol.infrastructure.adapter.in.rest.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.domain.exception.alimentacion.ExistenciasAlimentoInsuficientesException;
import com.avicontrol.domain.exception.alimentacion.SuministroDuplicadoException;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void mapeaExcepcionesDeDominioACodigosHttpYProblemJson() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
    String corr = UUID.randomUUID().toString();
    request.setAttribute("correlationId", corr);

    ResponseEntity<ApiErrorResponse> r404 =
        handler.handleDomain(new RecursoNoEncontradoException("No encontrado"), request);
    assertThat(r404.getStatusCode().value()).isEqualTo(404);
    assertThat(r404.getHeaders().getContentType().toString()).isEqualTo("application/problem+json");
    assertThat(r404.getBody().code()).isEqualTo("RECURSO_NO_ENCONTRADO");
    assertThat(r404.getBody().correlationId()).isEqualTo(corr);

    ResponseEntity<ApiErrorResponse> r422 =
        handler.handleDomain(new ExistenciasAlimentoInsuficientesException(), request);
    assertThat(r422.getStatusCode().value()).isEqualTo(422);
    assertThat(r422.getBody().code()).isEqualTo("EXISTENCIAS_INSUFICIENTES");

    ResponseEntity<ApiErrorResponse> r503 =
        handler.handleDomain(new Modulo1NoDisponibleException(), request);
    assertThat(r503.getStatusCode().value()).isEqualTo(503);
    assertThat(r503.getBody().code()).isEqualTo("MODULO1_NO_DISPONIBLE");

    ResponseEntity<ApiErrorResponse> r409Rec =
        handler.handleDomain(new RecepcionNoEditableException("Con consumo"), request);
    assertThat(r409Rec.getStatusCode().value()).isEqualTo(409);
    assertThat(r409Rec.getBody().code()).isEqualTo("CONFLICTO_RECEPCION");

    ResponseEntity<ApiErrorResponse> r409Sum =
        handler.handleDomain(new SuministroDuplicadoException(), request);
    assertThat(r409Sum.getStatusCode().value()).isEqualTo(409);
    assertThat(r409Sum.getBody().code()).isEqualTo("CONFLICTO_SUMINISTRO_ALIMENTO");
  }

  @Test
  void mapeaExcepcionesEstandarYPersistenciaConCorrelationId() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/recurso");

    ResponseEntity<ApiErrorResponse> integrity = handler.handleConflict(request);
    assertThat(integrity.getStatusCode().value()).isEqualTo(409);
    assertThat(integrity.getBody().code()).isEqualTo("CONFLICTO_INTEGRIDAD");
    assertThat(integrity.getBody().correlationId()).isNotBlank();

    ResponseEntity<ApiErrorResponse> dbDown = handler.handleStorageUnavailable(request);
    assertThat(dbDown.getStatusCode().value()).isEqualTo(503);
    assertThat(dbDown.getBody().code()).isEqualTo("PERSISTENCIA_NO_DISPONIBLE");

    ResponseEntity<ApiErrorResponse> badArg =
        handler.handleInvalid(new IllegalArgumentException("Dato inválido"), request);
    assertThat(badArg.getStatusCode().value()).isEqualTo(400);
    assertThat(badArg.getBody().code()).isEqualTo("SOLICITUD_INVALIDA");

    ResponseEntity<ApiErrorResponse> absent =
        handler.handleAbsent(new NoSuchElementException("Ausente"), request);
    assertThat(absent.getStatusCode().value()).isEqualTo(404);
    assertThat(absent.getBody().code()).isEqualTo("RECURSO_NO_ENCONTRADO");

    ResponseEntity<ApiErrorResponse> state =
        handler.handleState(new IllegalStateException("Estado inválido"), request);
    assertThat(state.getStatusCode().value()).isEqualTo(409);
    assertThat(state.getBody().code()).isEqualTo("ESTADO_INCOMPATIBLE");
  }
}
