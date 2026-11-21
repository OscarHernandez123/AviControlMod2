package com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MedicacionDiariaGalponResponse(
    UUID galponId,
    String nombreGalpon,
    int aforoMaximo,
    String estadoGalpon,
    UUID loteId,
    String loteCodigo,
    Integer poblacionViva,
    Integer edadDias,
    LocalDate fecha,
    String estado,
    String mensaje,
    int cantidadTratamientos,
    List<TratamientoResponse> tratamientos) {
  public record TratamientoResponse(
      UUID diagnosticoId,
      String loteCodigo,
      String progreso,
      String enfermedad,
      String medicamento,
      String principioActivo,
      String dosisHoy,
      String viaAdministracion,
      boolean aplicaHoy) {}
}
