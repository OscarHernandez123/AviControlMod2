package com.avicontrol.domain.model.inventario;

public enum TipoMovimientoInventario {
  ENTRADA(1),
  SALIDA(-1),
  CONSUMO(-1),
  VENCIMIENTO(-1),
  ANULACION(-1),
  AJUSTE(0);
  private final int signo;

  TipoMovimientoInventario(int signo) {
    this.signo = signo;
  }

  public int signo() {
    return signo;
  }
}
