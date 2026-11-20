package com.avicontrol.application.galpon.result;

import java.util.UUID;

public record GalponListadoResult(UUID id, String nombre, String estado, LoteActual loteActual) {
  public GalponListadoResult(UUID id, String nombre, String estado) {
    this(id, nombre, estado, null);
  }

  public record LoteActual(UUID id, String nombre) {}
}
