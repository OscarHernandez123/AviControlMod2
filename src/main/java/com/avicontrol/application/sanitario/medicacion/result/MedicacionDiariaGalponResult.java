package com.avicontrol.application.sanitario.medicacion.result;

import com.avicontrol.domain.model.sanitario.medicacion.MedicacionDiaria;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MedicacionDiariaGalponResult(
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
    List<MedicacionDiaria> tratamientos) {
  public MedicacionDiariaGalponResult {
    tratamientos = List.copyOf(tratamientos);
  }
}
