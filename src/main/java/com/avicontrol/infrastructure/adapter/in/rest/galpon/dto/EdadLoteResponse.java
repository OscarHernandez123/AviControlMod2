package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import java.util.UUID;

public record EdadLoteResponse(UUID galponId, UUID loteId, EdadResponse edad, String mensaje) {

  public record EdadResponse(
      int diasTotales, int semanasCompletas, int diasRestantes, String texto) {}
}
