package com.avicontrol.application.sacrificio.sanitario;

import com.avicontrol.application.sacrificio.sanitario.result.OrdenSacrificioSanitarioResult;
import com.avicontrol.domain.port.out.sacrificio.sanitario.OrdenSacrificioSanitarioRepositoryPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarOrdenesSacrificioSanitarioUseCase {
  private final OrdenSacrificioSanitarioRepositoryPort ordenes;

  public ConsultarOrdenesSacrificioSanitarioUseCase(
      OrdenSacrificioSanitarioRepositoryPort ordenes) {
    this.ordenes = ordenes;
  }

  @Transactional(readOnly = true)
  public List<OrdenSacrificioSanitarioResult> listar() {
    return ordenes.listar().stream().map(OrdenSacrificioSanitarioResult::de).toList();
  }
}
