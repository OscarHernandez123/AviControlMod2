package com.avicontrol.application.galpon;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.galpon.GalponPage;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.ResumenGalponesFuente;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarEdadPorGalponUseCaseTest {

  @Test
  void calculatesTheAgeOfTheSingleCurrentLote() {
    UUID galponId = UUID.randomUUID();
    GalponQueryPort galpones =
        galpones(new Galpon(galponId, "Galpón 1", 8000, EstadoGalpon.PRODUCTIVO));
    Lote lote =
        new Lote(
            UUID.randomUUID(),
            "Lote 1",
            8000,
            7960,
            LocalDate.of(2026, 10, 15),
            BigDecimal.TEN,
            galponId);

    var result =
        new ConsultarEdadPorGalponUseCase(galpones, ignored -> Optional.of(lote), admin(), clock())
            .execute(galponId);

    assertThat(result.edad().texto()).isEqualTo("2 semanas y 3 días (17 días)");
  }

  private GalponQueryPort galpones(Galpon galpon) {
    return new GalponQueryPort() {
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
  }

  private com.avicontrol.application.support.CurrentActorProvider admin() {
    return () -> new CurrentActor("admin", java.util.Set.of("ROLE_ADMINISTRADOR"));
  }

  private Clock clock() {
    return Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC);
  }
}
