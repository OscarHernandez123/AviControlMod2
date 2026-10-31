package com.avicontrol.infrastructure.adapter.out.internal.galpon;

import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.model.lote.Lote;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Development/test replacement for Module 1. It is intentionally read-only from the API surface.
 * Test fixtures or development bootstrap code may replace its snapshot through {@link
 * #replaceData}.
 */
@Component
public class InMemoryModulo1GalponesReadApi implements Modulo1GalponesReadApi {

  private final Map<UUID, Galpon> galpones = new HashMap<>();
  private final Map<UUID, List<Lote>> lotesActualesPorGalpon = new HashMap<>();
  private long inconsistentGalponRecords;

  public synchronized void replaceData(
      Collection<Galpon> nuevosGalpones,
      Collection<Lote> nuevosLotesActuales,
      long inconsistentes) {
    galpones.clear();
    lotesActualesPorGalpon.clear();
    nuevosGalpones.forEach(galpon -> galpones.put(galpon.id(), galpon));
    nuevosLotesActuales.forEach(
        lote ->
            lotesActualesPorGalpon
                .computeIfAbsent(lote.galponId(), ignored -> new ArrayList<>())
                .add(lote));
    inconsistentGalponRecords = inconsistentes;
  }

  @Override
  public synchronized Optional<Galpon> findGalponById(UUID galponId) {
    return Optional.ofNullable(galpones.get(galponId));
  }

  @Override
  public synchronized List<Galpon> findAllGalpones() {
    return galpones.values().stream().sorted(Comparator.comparing(Galpon::nombre)).toList();
  }

  @Override
  public synchronized List<Lote> findCurrentLotesByGalponId(UUID galponId) {
    return List.copyOf(lotesActualesPorGalpon.getOrDefault(galponId, List.of()));
  }

  @Override
  public synchronized long inconsistentGalponRecords() {
    return inconsistentGalponRecords;
  }
}
