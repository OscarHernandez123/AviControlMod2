package com.avicontrol.domain.model.sacrificio.productivo;

public enum EstadoOrdenSacrificio {
  PROGRAMADA,
  REPROGRAMADA,
  CANCELADA,
  EJECUTADA;

  public boolean pendiente() {
    return this == PROGRAMADA || this == REPROGRAMADA;
  }
}
