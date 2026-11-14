package com.avicontrol.domain.port.out.sacrificio.productivo;

import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdenSacrificioProductivoRepositoryPort {
  void crear(OrdenSacrificioProductivo orden);

  Optional<OrdenSacrificioProductivo> buscar(UUID id);

  Optional<OrdenSacrificioProductivo> bloquear(UUID id);

  boolean existePorGalponLote(UUID galponId, UUID loteId);

  boolean actualizar(OrdenSacrificioProductivo orden);

  List<UUID> vencidas(Instant ahora);

  Resumen resumen();

  record Resumen(long totalOrdenes, long ordenesPendientes) {}
}
