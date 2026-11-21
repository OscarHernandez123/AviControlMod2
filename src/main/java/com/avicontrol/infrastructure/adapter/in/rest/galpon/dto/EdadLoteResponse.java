package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import java.util.UUID;

public record EdadLoteResponse(UUID galponId, UUID loteId, EdadResponse edad, String mensaje) {
  public record EdadResponse(
      long diasTotales, long semanasCompletas, long diasRestantes, String texto) {}
}
