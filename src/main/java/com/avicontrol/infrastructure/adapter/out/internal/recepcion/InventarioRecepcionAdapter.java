package com.avicontrol.infrastructure.adapter.out.internal.recepcion;

import com.avicontrol.application.inventario.*;
import com.avicontrol.application.inventario.port.*;
import com.avicontrol.domain.port.out.recepcion.InventarioRecepcionPort;
import org.springframework.stereotype.Component;

@Component
class InventarioRecepcionAdapter implements InventarioRecepcionPort {
  private final RegistrarEntradaInventarioPort registrar;
  private final AjustarEntradaRecepcionPort ajustar;
  private final VerificarRecepcionUtilizadaPort verificar;

  InventarioRecepcionAdapter(
      RegistrarEntradaInventarioPort r,
      AjustarEntradaRecepcionPort a,
      VerificarRecepcionUtilizadaPort v) {
    registrar = r;
    ajustar = a;
    verificar = v;
  }

  public void registrarAlimento(java.util.UUID id, java.math.BigDecimal cantidad) {
    registrar.registrarEntradaAlimento(new EntradaAlimentoCommand(id, cantidad));
  }

  public void registrarMedicamento(
      java.util.UUID id, java.math.BigDecimal cantidad, String unidad) {
    registrar.registrarEntradaMedicamento(new EntradaMedicamentoCommand(id, cantidad, unidad));
  }

  public void ajustarAlimento(java.util.UUID id, java.math.BigDecimal cantidad) {
    ajustar.ajustarEntradaAlimento(new EntradaAlimentoCommand(id, cantidad));
  }

  public void ajustarMedicamento(java.util.UUID id, java.math.BigDecimal cantidad, String unidad) {
    ajustar.ajustarEntradaMedicamento(new EntradaMedicamentoCommand(id, cantidad, unidad));
  }

  public boolean alimentoUtilizado(java.util.UUID id) {
    return verificar.alimentoUtilizado(id);
  }

  public boolean medicamentoUtilizado(java.util.UUID id) {
    return verificar.medicamentoUtilizado(id);
  }
}
