package com.avicontrol.infrastructure.adapter.out.internal.galpon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.domain.exception.galpon.LoteActualInconsistenteException;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GalponLoteQueryAdaptersTest {

  @Test
  void paginatesAndRejectsMoreThanOneCurrentLote() {
    UUID galponId = UUID.randomUUID();
    Galpon galpon = new Galpon(galponId, "Galpón A", 8000, EstadoGalpon.PRODUCTIVO);
    Lote lote1 = lote(galponId);
    Lote lote2 = lote(galponId);
    InMemoryModulo1GalponesReadApi source = new InMemoryModulo1GalponesReadApi();
    source.replaceData(List.of(galpon), List.of(lote1, lote2), 0);

    assertThat(
            new GalponQueryAdapter(source)
                .listar(new GalponPageQuery(0, 20, "nombre", GalponPageQuery.SortDirection.ASC))
                .content())
        .containsExactly(galpon);
    assertThatThrownBy(() -> new LoteQueryAdapter(source).buscarActualPorGalponId(galponId))
        .isInstanceOf(LoteActualInconsistenteException.class);
  }

  private Lote lote(UUID galponId) {
    return new Lote(
        UUID.randomUUID(), "Lote", 8000, 8000, LocalDate.now(), BigDecimal.ONE, galponId);
  }
}
