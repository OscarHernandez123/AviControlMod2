package com.avicontrol.domain.port.out.sanitario.diagnostico;

import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosticoGalponRepositoryPort {
  void crear(DiagnosticoGalpon diagnostico);

  Optional<DiagnosticoGalpon> buscar(UUID id);

  Optional<DiagnosticoGalpon> bloquear(UUID id);

  boolean existeActivo(UUID galponId);

  boolean existeActivoMismaEnfermedad(UUID galponId, UUID enfermedadId);

  boolean existeSanitarioActivo(UUID galponId);

  boolean existeOtroActivo(UUID galponId, UUID diagnosticoId);

  List<UUID> vencidos(LocalDate fecha);

  boolean resolver(DiagnosticoGalpon anterior, DiagnosticoGalpon resuelto);
}
