package com.avicontrol.application.inventario.port;

import com.avicontrol.application.inventario.EntradaAlimentoCommand;
import com.avicontrol.application.inventario.EntradaMedicamentoCommand;

public interface RegistrarEntradaInventarioPort {
  void registrarEntradaAlimento(EntradaAlimentoCommand command);

  void registrarEntradaMedicamento(EntradaMedicamentoCommand command);
}
