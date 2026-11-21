package com.avicontrol.application.sanitario.enfermedad;

import com.avicontrol.application.sanitario.enfermedad.result.EnfermedadResult;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class ListarEnfermedadesUseCase {
  private final EnfermedadRepositoryPort repository;

  public ListarEnfermedadesUseCase(EnfermedadRepositoryPort repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<EnfermedadResult> listar() {
    return repository.listar().stream().map(EnfermedadResult::de).toList();
  }
}
