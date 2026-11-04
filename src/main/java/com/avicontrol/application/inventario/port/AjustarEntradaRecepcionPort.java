package com.avicontrol.application.inventario.port;

import com.avicontrol.application.inventario.EntradaAlimentoCommand;
import com.avicontrol.application.inventario.EntradaMedicamentoCommand;

public interface AjustarEntradaRecepcionPort {
  void ajustarEntradaAlimento(EntradaAlimentoCommand command);

  void ajustarEntradaMedicamento(EntradaMedicamentoCommand command);
}
