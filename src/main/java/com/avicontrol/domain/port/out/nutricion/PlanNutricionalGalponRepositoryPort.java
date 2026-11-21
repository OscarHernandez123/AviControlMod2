package com.avicontrol.domain.port.out.nutricion;

import com.avicontrol.domain.model.nutricion.AuditoriaPlanNutricional;
import com.avicontrol.domain.model.nutricion.PlanNutricionalGalpon;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanNutricionalGalponRepositoryPort {
  void crear(PlanNutricionalGalpon plan);

  void actualizar(PlanNutricionalGalpon plan);

  Optional<PlanNutricionalGalpon> buscarPorGalpon(UUID galponId);

  Optional<PlanNutricionalGalpon> buscarPorLote(UUID loteId);

  Optional<PlanNutricionalGalpon.Etapa> buscarEtapa(UUID etapaId);

  void registrarAuditoria(UUID planId, AuditoriaPlanNutricional auditoria);

  void registrarAjuste(
      UUID etapaId,
      String actor,
      String motivo,
      String justificacion,
      int diaFinAnterior,
      int diaFinNuevo,
      Instant fecha);

  List<AuditoriaPlanNutricional> historial(UUID planId);

  List<UUID> lotesActivos();
}
