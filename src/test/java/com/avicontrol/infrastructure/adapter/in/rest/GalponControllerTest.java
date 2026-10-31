package com.avicontrol.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.application.galpon.ConsultarEdadPorGalponUseCase;
import com.avicontrol.application.galpon.ConsultarGalponUseCase;
import com.avicontrol.application.galpon.ConsultarListadoGalponesUseCase;
import com.avicontrol.application.galpon.ConsultarResumenGalponesUseCase;
import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.port.out.galpon.GalponPage;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.ResumenGalponesFuente;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.GalponController;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.mapper.GalponRestMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GalponControllerTest {

  @Test
  void mapsTheEmptyListContractWithoutExposingDomainObjects() {
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
            return new ResumenGalponesFuente(0, 0, java.util.Map.of());
          }
        };
    var actor =
        (com.avicontrol.application.support.CurrentActorProvider)
            () -> new CurrentActor("worker", java.util.Set.of("ROLE_TRABAJADOR"));
    var controller =
        new GalponController(
            new ConsultarEdadPorGalponUseCase(
                port,
                ignored -> Optional.empty(),
                actor,
                Clock.fixed(Instant.now(), ZoneOffset.UTC)),
            new ConsultarGalponUseCase(
                port,
                ignored -> Optional.empty(),
                actor,
                Clock.fixed(Instant.now(), ZoneOffset.UTC)),
            new ConsultarResumenGalponesUseCase(port, actor),
            new ConsultarListadoGalponesUseCase(port, actor),
            new GalponRestMapper());

    var response = controller.listar(0, 20, "nombre,asc");

    assertThat(response.getBody().content()).isEmpty();
    assertThat(response.getBody().mensaje()).isEqualTo("No existen galpones disponibles");
  }
}
