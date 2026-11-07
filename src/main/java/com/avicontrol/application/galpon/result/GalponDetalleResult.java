package com.avicontrol.application.galpon.result;

import com.avicontrol.domain.model.lote.Edad;
import java.util.UUID;

public record GalponDetalleResult(
    UUID id, String nombre, int aforoMaximo, String estado, LoteActual loteActual, String mensaje) {
  public record LoteActual(UUID id, int poblacionActual, Edad edad, String mensaje) {}
}
