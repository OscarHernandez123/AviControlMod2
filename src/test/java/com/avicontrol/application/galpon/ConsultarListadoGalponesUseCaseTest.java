package com.avicontrol.application.galpon;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.port.out.galpon.GalponPage;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.ResumenGalponesFuente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarListadoGalponesUseCaseTest {

  @Test
  void providesStablePaginationMetadataToAWorker() {
    Galpon galpon = new Galpon(UUID.randomUUID(), "Galpón 1", 8000, EstadoGalpon.PRODUCTIVO);
    GalponQueryPort port =
        new GalponQueryPort() {
          @Override
          public Optional<Galpon> buscarPorId(UUID id) {
            return Optional.empty();
          }

          @Override
          public GalponPage listar(GalponPageQuery query) {
            return new GalponPage(List.of(galpon), 1);
          }

          @Override
          public ResumenGalponesFuente consultarResumen() {
            return new ResumenGalponesFuente(0, 0, java.util.Map.of());
          }
        };

    var result =
        new ConsultarListadoGalponesUseCase(
                port, () -> new CurrentActor("worker", java.util.Set.of("ROLE_TRABAJADOR")))
            .execute(0, 20, "nombre,asc");

    assertThat(result.content()).singleElement().extracting("id").isEqualTo(galpon.id());
    assertThat(result.first()).isTrue();
    assertThat(result.last()).isTrue();
  }
}
