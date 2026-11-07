package com.avicontrol.domain.model.alimentacion;

public record AlertaAlimentacion(String tipo, String mensaje) {
  public AlertaAlimentacion {
    if (tipo == null || tipo.isBlank() || mensaje == null || mensaje.isBlank())
      throw new IllegalArgumentException("Alerta de alimentación inválida");
  }
}
