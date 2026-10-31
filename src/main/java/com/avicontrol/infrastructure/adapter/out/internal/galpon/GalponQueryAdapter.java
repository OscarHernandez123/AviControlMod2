package com.avicontrol.infrastructure.adapter.out.internal.galpon;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.port.out.galpon.GalponPage;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.ResumenGalponesFuente;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GalponQueryAdapter implements GalponQueryPort {

  private final Modulo1GalponesReadApi modulo1GalponesReadApi;

  public GalponQueryAdapter(Modulo1GalponesReadApi modulo1GalponesReadApi) {
    this.modulo1GalponesReadApi = modulo1GalponesReadApi;
  }

  @Override
  public Optional<Galpon> buscarPorId(UUID galponId) {
    return modulo1GalponesReadApi.findGalponById(galponId);
  }

  @Override
  public GalponPage listar(GalponPageQuery query) {
    Comparator<Galpon> comparator =
        query.sortProperty().equals("estado")
            ? Comparator.comparing(galpon -> galpon.estado().name())
            : Comparator.comparing(Galpon::nombre, String.CASE_INSENSITIVE_ORDER);
    if (query.direction() == GalponPageQuery.SortDirection.DESC) {
      comparator = comparator.reversed();
    }
    List<Galpon> all =
        modulo1GalponesReadApi.findAllGalpones().stream().sorted(comparator).toList();
    int from = Math.min(query.offset(), all.size());
    int to = Math.min(from + query.limit(), all.size());
    return new GalponPage(all.subList(from, to), all.size());
  }

  @Override
  public ResumenGalponesFuente consultarResumen() {
    Map<EstadoGalpon, Long> counts = new EnumMap<>(EstadoGalpon.class);
    for (EstadoGalpon estado : EstadoGalpon.values()) {
      counts.put(estado, 0L);
    }
    for (Galpon galpon : modulo1GalponesReadApi.findAllGalpones()) {
      counts.compute(galpon.estado(), (estado, count) -> count + 1);
    }
    long inconsistentes = modulo1GalponesReadApi.inconsistentGalponRecords();
    return new ResumenGalponesFuente(
        counts.values().stream().mapToLong(Long::longValue).sum() + inconsistentes,
        inconsistentes,
        counts);
  }
}
