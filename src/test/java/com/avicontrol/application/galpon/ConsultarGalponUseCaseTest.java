package com.avicontrol.application.galpon;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.port.out.galpon.GalponPage;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.ResumenGalponesFuente;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarGalponUseCaseTest {

  @Test
  void preservesGalponDataAndReportsTheAbsenceOfACurrentLote() {
    UUID id = UUID.randomUUID();
    Galpon galpon = new Galpon(id, "Galpón vacío", 6000, EstadoGalpon.DISPONIBLE);
    GalponQueryPort port =
        new GalponQueryPort() {
          @Override
          public Optional<Galpon> buscarPorId(UUID ignored) {
            return Optional.of(galpon);
          }

          @Override
          public GalponPage listar(GalponPageQuery query) {
            return new GalponPage(List.of(), 0);
          }

          @Override
          public ResumenGalponesFuente consultarResumen() {
            return new ResumenGalponesFuente(0, 0, java.util.Map.of());
          }
        };

    var result =
        new ConsultarGalponUseCase(
                port,
                ignored -> Optional.empty(),
                () -> new CurrentActor("worker", java.util.Set.of("ROLE_TRABAJADOR")),
                Clock.fixed(Instant.now(), ZoneOffset.UTC))
            .execute(id);

    assertThat(result.nombre()).isEqualTo("Galpón vacío");
    assertThat(result.loteActual()).isNull();
    assertThat(result.incidencias())
        .singleElement()
        .extracting("codigo")
        .isEqualTo("GALPON_SIN_LOTE_ACTUAL");
  }
}
