package com.avicontrol.application.sanitario.medicacion;

import com.avicontrol.application.sanitario.medicacion.result.MedicacionResult;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class ListarMedicacionesUseCase {
  private final MedicacionRepositoryPort repository;

  public ListarMedicacionesUseCase(MedicacionRepositoryPort repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<MedicacionResult> listar() {
    return repository.listar().stream().map(MedicacionResult::de).toList();
  }
}
