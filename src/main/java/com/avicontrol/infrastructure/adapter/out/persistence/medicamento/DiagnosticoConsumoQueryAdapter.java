package com.avicontrol.infrastructure.adapter.out.persistence.medicamento;

import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.port.out.medicamento.DiagnosticoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DiagnosticoConsumoQueryAdapter implements DiagnosticoQueryPort {
  private final DiagnosticoGalponRepositoryPort diagnosticos;

  public DiagnosticoConsumoQueryAdapter(DiagnosticoGalponRepositoryPort diagnosticos) {
    this.diagnosticos = diagnosticos;
  }

  @Override
  public Optional<DiagnosticoGalpon> buscar(UUID id) {
    return diagnosticos.buscar(id);
  }
}
