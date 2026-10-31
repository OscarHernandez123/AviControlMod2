package com.avicontrol.infrastructure.error;

import com.avicontrol.domain.exception.BusinessRuleViolationException;
import com.avicontrol.domain.exception.ConflictException;
import com.avicontrol.domain.exception.DomainException;
import com.avicontrol.domain.exception.ResourceNotFoundException;
import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ProblemDetail> handleNotFound(
      ResourceNotFoundException exception, HttpServletRequest request) {
    return problem(HttpStatus.NOT_FOUND, exception, request, List.of());
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<ProblemDetail> handleConflict(
      ConflictException exception, HttpServletRequest request) {
    return problem(HttpStatus.CONFLICT, exception, request, List.of());
  }

  @ExceptionHandler(BusinessRuleViolationException.class)
  ResponseEntity<ProblemDetail> handleBusinessRule(
      BusinessRuleViolationException exception, HttpServletRequest request) {
    return problem(HttpStatus.UNPROCESSABLE_CONTENT, exception, request, List.of());
  }

  @ExceptionHandler(InformacionGalponNoDisponibleException.class)
  ResponseEntity<ProblemDetail> handleUnavailable(
      InformacionGalponNoDisponibleException exception, HttpServletRequest request) {
    return problem(HttpStatus.SERVICE_UNAVAILABLE, exception, request, List.of());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        new BusinessRuleViolationException(
            "PARAMETRO_INVALIDO", "Uno o más parámetros son inválidos"),
        request,
        List.of(new ApiFieldError(exception.getName(), "El formato del parámetro no es válido")));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiFieldError> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream().map(this::toApiFieldError).toList();
    return problem(
        HttpStatus.BAD_REQUEST,
        new BusinessRuleViolationException("CAMPOS_INVALIDOS", "Uno o más campos son inválidos"),
        request,
        fieldErrors);
  }

  private ApiFieldError toApiFieldError(FieldError fieldError) {
    return new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage());
  }

  private ResponseEntity<ProblemDetail> problem(
      HttpStatus status,
      DomainException exception,
      HttpServletRequest request,
      List<ApiFieldError> fieldErrors) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    problem.setType(URI.create("https://avicontrol/errors/" + exception.code().toLowerCase()));
    problem.setTitle(titleFor(status));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", exception.code());
    problem.setProperty("correlationId", request.getAttribute("correlationId"));
    problem.setProperty("fieldErrors", fieldErrors);
    return ResponseEntity.status(status)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        .body(problem);
  }

  private String titleFor(HttpStatus status) {
    return switch (status) {
      case NOT_FOUND -> "Recurso no encontrado";
      case CONFLICT -> "Conflicto de estado";
      case UNPROCESSABLE_CONTENT -> "Regla de negocio incumplida";
      case SERVICE_UNAVAILABLE -> "Información no disponible";
      default -> "Solicitud inválida";
    };
  }
}
