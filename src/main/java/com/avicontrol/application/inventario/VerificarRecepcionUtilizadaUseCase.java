package com.avicontrol.application.inventario;

import com.avicontrol.application.inventario.port.VerificarRecepcionUtilizadaPort;
import com.avicontrol.domain.port.out.inventario.MovimientoAlimentoRepositoryPort;
import com.avicontrol.domain.port.out.inventario.MovimientoMedicamentoRepositoryPort;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class VerificarRecepcionUtilizadaUseCase implements VerificarRecepcionUtilizadaPort {
  private final MovimientoAlimentoRepositoryPort alimentos;
  private final MovimientoMedicamentoRepositoryPort medicamentos;

  public VerificarRecepcionUtilizadaUseCase(
      MovimientoAlimentoRepositoryPort alimentos,
      MovimientoMedicamentoRepositoryPort medicamentos) {
    this.alimentos = alimentos;
    this.medicamentos = medicamentos;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean alimentoUtilizado(UUID recepcionId) {
    return alimentos.tieneMovimientosPosterioresConfirmados(recepcionId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean medicamentoUtilizado(UUID recepcionId) {
    return medicamentos.tieneMovimientosPosterioresConfirmados(recepcionId);
  }
}
