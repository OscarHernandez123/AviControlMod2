package com.avicontrol.application.inventario.port;

import java.util.UUID;

/** Contrato público de Inventario para proteger la edición de una recepción. */
public interface VerificarRecepcionUtilizadaPort {
  boolean alimentoUtilizado(UUID recepcionId);

  boolean medicamentoUtilizado(UUID recepcionId);
}
