package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GalponDetalleResponse(
    UUID id,
    String nombre,
    int aforoMaximo,
    EstadoGalpon estado,
    LoteActualResponse loteActual,
    List<IncidenciaResponse> incidencias) {

  public record LoteActualResponse(
      UUID id, int poblacionActual, LocalDate fechaIngreso, EdadLoteResponse.EdadResponse edad) {}

  public record IncidenciaResponse(String codigo, String mensaje) {}
}
