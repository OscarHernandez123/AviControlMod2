package com.avicontrol.infrastructure.adapter.in.rest.shared;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Map<String, SqlError> SQL_ERRORS =
      Map.of(
          "P0001",
          new SqlError(
              HttpStatus.CONFLICT, "OPERACION_PROHIBIDA", "La operación no está permitida."),
          "P0002",
          new SqlError(
              HttpStatus.NOT_FOUND, "SOLICITUD_NO_ENCONTRADA", "No se encontró la solicitud."),
          "P0003",
          new SqlError(
              HttpStatus.CONFLICT, "SOLICITUD_YA_RESUELTA", "La solicitud ya fue resuelta."),
          "P0004",
          new SqlError(
              HttpStatus.CONFLICT,
              "CONCURRENCIA_OPTIMISTA",
              "El registro cambió; actualice la información e intente nuevamente."),
          "P0005",
          new SqlError(
              HttpStatus.CONFLICT,
              "ESTADO_GALPON_INCOMPATIBLE",
              "El estado actual del galpón no permite esta operación."),
          "P0006",
          new SqlError(
              HttpStatus.CONFLICT,
              "EXISTENCIAS_INSUFICIENTES",
              "No hay existencias suficientes para completar la operación."),
          "P0007",
          new SqlError(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA", "No se encontró la orden."),
          "P0008",
          new SqlError(HttpStatus.CONFLICT, "ORDEN_YA_EJECUTADA", "La orden ya fue ejecutada."),
          "P0009",
          new SqlError(
              HttpStatus.CONFLICT,
              "GALPON_NO_ESTA_EN_AISLAMIENTO",
              "El galpón no está en aislamiento."),
          "P0010",
          new SqlError(
              HttpStatus.CONFLICT, "LOTE_SIN_POBLACION_VIVA", "El lote no tiene población viva."));

  private final ApiProblemFactory problemFactory;

  public GlobalExceptionHandler(ApiProblemFactory problemFactory) {
    this.problemFactory = problemFactory;
  }

  @ExceptionHandler({DataAccessException.class, SQLException.class})
  public ResponseEntity<ApiProblem> handleDatabaseException(
      Exception exception, HttpServletRequest request) {
    SQLException sqlException = findSqlException(exception);
    SqlError sqlError = sqlException == null ? null : SQL_ERRORS.get(sqlException.getSQLState());
    if (sqlError != null) {
      return response(request, sqlError.status(), sqlError.code(), sqlError.detail());
    }

    return response(
        request,
        HttpStatus.INTERNAL_SERVER_ERROR,
        "ERROR_BASE_DATOS",
        "No fue posible completar la operación en la base de datos.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiProblem> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    SQLException sqlException = findSqlException(exception);
    if (sqlException != null) {
      SqlError sqlError = SQL_ERRORS.get(sqlException.getSQLState());
      if (sqlError != null) {
        return response(request, sqlError.status(), sqlError.code(), sqlError.detail());
      }
      return response(
          request,
          HttpStatus.INTERNAL_SERVER_ERROR,
          "ERROR_BASE_DATOS",
          "No fue posible completar la operación en la base de datos.");
    }

    return response(
        request, HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error interno.");
  }

  private ResponseEntity<ApiProblem> response(
      HttpServletRequest request, HttpStatus status, String code, String detail) {
    ApiProblem problem = problemFactory.create(request, status, code, detail);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .header("X-Correlation-Id", problem.correlationId())
        .body(problem);
  }

  private SQLException findSqlException(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof SQLException sqlException) {
        return sqlException;
      }
      current = current.getCause();
    }
    return null;
  }

  private record SqlError(HttpStatus status, String code, String detail) {}
}
