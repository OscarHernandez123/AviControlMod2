package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import java.util.UUID;

public record GalponDetalleResponse(
    UUID id,
    String nombre,
    int aforoMaximo,
    String estado,
    LoteActualResponse loteActual,
    String mensaje) {
  public record LoteActualResponse(
      UUID id, int poblacionActual, EdadLoteResponse.EdadResponse edad, String mensaje) {}
}
