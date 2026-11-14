package com.avicontrol.infrastructure.adapter.in.rest.error;

import com.avicontrol.domain.exception.DomainException;
import com.avicontrol.domain.exception.alimentacion.AlimentoNoCorrespondeEtapaException;
import com.avicontrol.domain.exception.alimentacion.ExistenciasAlimentoInsuficientesException;
import com.avicontrol.domain.exception.alimentacion.SuministroDuplicadoException;
import com.avicontrol.domain.exception.galpon.FechaIngresoInconsistenteException;
import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.exception.galpon.LoteActualInconsistenteException;
import com.avicontrol.domain.exception.medicamento.ConsumoMedicamentoNoEncontradoException;
import com.avicontrol.domain.exception.medicamento.DiagnosticoConsumoNoEncontradoException;
import com.avicontrol.domain.exception.medicamento.ExistenciasMedicamentoInsuficientesException;
import com.avicontrol.domain.exception.medicamento.IdempotenciaConsumoIncompatibleException;
import com.avicontrol.domain.exception.medicamento.TratamientoNoActivoException;
import com.avicontrol.domain.exception.medicamento.UnidadMedicamentoIncompatibleException;
import com.avicontrol.domain.exception.recepcion.FechaVencimientoInvalidaException;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.exception.recepcion.UnidadIncompatibleException;
import com.avicontrol.domain.exception.sacrificio.productivo.FechaProgramadaInvalidaException;
import com.avicontrol.domain.exception.sacrificio.productivo.GalponNoAptoParaSacrificioException;
import com.avicontrol.domain.exception.sacrificio.productivo.InformacionAptitudNoDisponibleException;
import com.avicontrol.domain.exception.sacrificio.productivo.LoteActualNoEncontradoException;
import com.avicontrol.domain.exception.sacrificio.productivo.OrdenNoModificableException;
import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.exception.sacrificio.productivo.ResultadoSacrificioDuplicadoException;
import com.avicontrol.domain.exception.sacrificio.productivo.ResultadoSacrificioNoModificableException;
import com.avicontrol.domain.exception.sacrificio.productivo.ValoresResultadoSacrificioInvalidosException;
import com.avicontrol.domain.exception.sacrificio.sanitario.DiagnosticoNoElegibleException;
import com.avicontrol.domain.exception.sacrificio.sanitario.OrdenSanitariaNoPendienteException;
import com.avicontrol.domain.exception.sacrificio.sanitario.PoblacionNoSacrificableException;
import com.avicontrol.domain.exception.sanitario.aislamiento.EstadoGalponNoAislableException;
import com.avicontrol.domain.exception.sanitario.aislamiento.GalponNoEncontradoException;
import com.avicontrol.domain.exception.sanitario.aislamiento.LoteActivoNoEncontradoException;
import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoDuplicadaException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoNoEncontradaException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoNoPendienteException;
import com.avicontrol.domain.exception.sanitario.diagnostico.DiagnosticoActivoExistenteException;
import com.avicontrol.domain.exception.sanitario.diagnostico.GalponNoAisladoException;
import com.avicontrol.domain.exception.sanitario.diagnostico.MedicacionNoCompatibleException;
import com.avicontrol.domain.exception.sanitario.enfermedad.EnfermedadNoEncontradaException;
import com.avicontrol.domain.exception.sanitario.medicacion.EnfermedadNoDisponibleException;
import com.avicontrol.domain.exception.sanitario.medicacion.MedicacionNoEncontradaException;
import com.avicontrol.domain.exception.sanitario.medicacion.MedicamentoNoDisponibleException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<String> fields =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
    return response(
        HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", "Campos inválidos", fields, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiErrorResponse> handleMalformed(HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", "JSON inválido", List.of(), request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiErrorResponse> handleTypeMismatch(HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST,
        "SOLICITUD_INVALIDA",
        "Parámetro de ruta o consulta inválido",
        List.of(),
        request);
  }

  @ExceptionHandler(DomainException.class)
  ResponseEntity<ApiErrorResponse> handleDomain(
      DomainException exception, HttpServletRequest request) {
    if (exception instanceof Modulo1NoDisponibleException
        || exception instanceof InformacionGalponNoDisponibleException) {
      return response(
          HttpStatus.SERVICE_UNAVAILABLE,
          "MODULO1_NO_DISPONIBLE",
          exception.getMessage(),
          List.of(),
          request);
    }
    if (exception instanceof ExistenciasMedicamentoInsuficientesException
        || exception instanceof ExistenciasAlimentoInsuficientesException) {
      return response(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "EXISTENCIAS_INSUFICIENTES",
          exception.getMessage(),
          List.of(),
          request);
    }
    if (exception instanceof RecursoNoEncontradoException
        || exception instanceof ConsumoMedicamentoNoEncontradoException
        || exception instanceof DiagnosticoConsumoNoEncontradoException
        || exception instanceof GalponNoEncontradoException
        || exception instanceof com.avicontrol.domain.exception.galpon.GalponNoEncontradoException
        || exception instanceof LoteActivoNoEncontradoException
        || exception instanceof LoteActualNoEncontradoException
        || exception
            instanceof com.avicontrol.domain.exception.alimentacion.LoteActivoNoEncontradoException
        || exception instanceof SolicitudAislamientoNoEncontradaException
        || exception instanceof EnfermedadNoEncontradaException
        || exception instanceof MedicacionNoEncontradaException
        || exception instanceof EnfermedadNoDisponibleException
        || exception instanceof MedicamentoNoDisponibleException) {
      return response(
          HttpStatus.NOT_FOUND,
          "RECURSO_NO_ENCONTRADO",
          exception.getMessage(),
          List.of(),
          request);
    }
    if (exception instanceof SolicitudAislamientoDuplicadaException
        || exception instanceof SolicitudAislamientoNoPendienteException
        || exception instanceof EstadoGalponNoAislableException) {
      return response(
          HttpStatus.CONFLICT, "CONFLICTO_AISLAMIENTO", exception.getMessage(), List.of(), request);
    }
    if (exception instanceof FechaProgramadaInvalidaException
        || exception instanceof ValoresResultadoSacrificioInvalidosException) {
      return response(
          HttpStatus.BAD_REQUEST, "FECHA_INVALIDA", exception.getMessage(), List.of(), request);
    }
    if (exception instanceof FechaIngresoInconsistenteException
        || exception instanceof LoteActualInconsistenteException
        || exception instanceof GalponNoAptoParaSacrificioException
        || exception instanceof InformacionAptitudNoDisponibleException
        || exception instanceof OrdenNoModificableException
        || exception instanceof PrecondicionSacrificioNoCumplidaException
        || exception instanceof ResultadoSacrificioDuplicadoException
        || exception instanceof ResultadoSacrificioNoModificableException) {
      return response(
          HttpStatus.CONFLICT, "CONFLICTO_GALPON", exception.getMessage(), List.of(), request);
    }
    if (exception instanceof DiagnosticoActivoExistenteException
        || exception instanceof GalponNoAisladoException
        || exception instanceof MedicacionNoCompatibleException) {
      return response(
          HttpStatus.CONFLICT, "CONFLICTO_DIAGNOSTICO", exception.getMessage(), List.of(), request);
    }
    if (exception instanceof DiagnosticoNoElegibleException
        || exception instanceof OrdenSanitariaNoPendienteException
        || exception instanceof PoblacionNoSacrificableException) {
      return response(
          HttpStatus.CONFLICT,
          "CONFLICTO_SACRIFICIO_SANITARIO",
          exception.getMessage(),
          List.of(),
          request);
    }
    if (exception instanceof TratamientoNoActivoException
        || exception instanceof UnidadMedicamentoIncompatibleException
        || exception instanceof IdempotenciaConsumoIncompatibleException) {
      return response(
          HttpStatus.CONFLICT,
          "CONFLICTO_CONSUMO_MEDICAMENTO",
          exception.getMessage(),
          List.of(),
          request);
    }
    if (exception instanceof AlimentoNoCorrespondeEtapaException
        || exception instanceof SuministroDuplicadoException) {
      return response(
          HttpStatus.CONFLICT,
          "CONFLICTO_SUMINISTRO_ALIMENTO",
          exception.getMessage(),
          List.of(),
          request);
    }
    if (exception instanceof UnidadIncompatibleException
        || exception instanceof RecepcionNoEditableException
        || exception instanceof FechaVencimientoInvalidaException) {
      return response(
          HttpStatus.CONFLICT, "CONFLICTO_RECEPCION", exception.getMessage(), List.of(), request);
    }
    return response(
        HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", exception.getMessage(), List.of(), request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConflict(HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT,
        "CONFLICTO_INTEGRIDAD",
        "El recurso ya existe o viola una restricción de integridad",
        List.of(),
        request);
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<ApiErrorResponse> handleStorageUnavailable(HttpServletRequest request) {
    return response(
        HttpStatus.SERVICE_UNAVAILABLE,
        "PERSISTENCIA_NO_DISPONIBLE",
        "No se pudo completar la operación de persistencia",
        List.of(),
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> handleInvalid(
      IllegalArgumentException exception, HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", exception.getMessage(), List.of(), request);
  }

  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<ApiErrorResponse> handleAbsent(
      NoSuchElementException exception, HttpServletRequest request) {
    return response(
        HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", exception.getMessage(), List.of(), request);
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<ApiErrorResponse> handleState(
      IllegalStateException exception, HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT, "ESTADO_INCOMPATIBLE", exception.getMessage(), List.of(), request);
  }

  private ResponseEntity<ApiErrorResponse> response(
      HttpStatus status,
      String code,
      String detail,
      List<String> fieldErrors,
      HttpServletRequest request) {
    String correlationId = (String) request.getAttribute("correlationId");
    if (correlationId == null) correlationId = UUID.randomUUID().toString();
    ApiErrorResponse body =
        new ApiErrorResponse(
            URI.create("https://avicontrol/errors/" + code.toLowerCase().replace('_', '-')),
            status.getReasonPhrase(),
            status.value(),
            detail,
            request.getRequestURI(),
            code,
            correlationId,
            fieldErrors);
    return ResponseEntity.status(status)
        .contentType(MediaType.parseMediaType("application/problem+json"))
        .body(body);
  }
}
