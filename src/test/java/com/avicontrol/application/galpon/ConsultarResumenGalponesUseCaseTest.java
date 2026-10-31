package com.avicontrol.application.galpon;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.port.out.galpon.GalponPage;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.ResumenGalponesFuente;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarResumenGalponesUseCaseTest {

  @Test
  void returnsAllSixStatesIncludingZeroCounts() {
    var counts = new EnumMap<EstadoGalpon, Long>(EstadoGalpon.class);
    counts.put(EstadoGalpon.PRODUCTIVO, 2L);
    GalponQueryPort port =
        new GalponQueryPort() {
          @Override
          public Optional<Galpon> buscarPorId(UUID id) {
            return Optional.empty();
          }

          @Override
          public GalponPage listar(GalponPageQuery query) {
            return new GalponPage(List.of(), 0);
          }

          @Override
          public ResumenGalponesFuente consultarResumen() {
            return new ResumenGalponesFuente(2, 0, counts);
          }
        };

    var result =
        new ConsultarResumenGalponesUseCase(
                port, () -> new CurrentActor("admin", java.util.Set.of("ROLE_ADMINISTRADOR")))
            .execute();

    assertThat(result.porEstado()).hasSize(6).containsEntry(EstadoGalpon.AISLAMIENTO, 0L);
  }
}
