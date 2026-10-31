package com.avicontrol.infrastructure.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.application.galpon.ConsultarGalponUseCase;
import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.infrastructure.adapter.out.internal.galpon.GalponQueryAdapter;
import com.avicontrol.infrastructure.adapter.out.internal.galpon.InMemoryModulo1GalponesReadApi;
import com.avicontrol.infrastructure.adapter.out.internal.galpon.LoteQueryAdapter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultaGalponesIntegrationTest {

  @Test
  void reflectsTheCurrentSnapshotWithoutWritingANewProjection() {
    UUID galponId = UUID.randomUUID();
    var source = new InMemoryModulo1GalponesReadApi();
    source.replaceData(
        List.of(new Galpon(galponId, "Galpón 1", 8000, EstadoGalpon.PRODUCTIVO)),
        List.of(
            new Lote(
                UUID.randomUUID(),
                "Lote 1",
                8000,
                7960,
                LocalDate.of(2026, 10, 15),
                BigDecimal.TEN,
                galponId)),
        0);
    var useCase =
        new ConsultarGalponUseCase(
            new GalponQueryAdapter(source),
            new LoteQueryAdapter(source),
            () -> new CurrentActor("worker", java.util.Set.of("ROLE_TRABAJADOR")),
            Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC));

    var result = useCase.execute(galponId);

    assertThat(result.loteActual().poblacionActual()).isEqualTo(7960);
    assertThat(result.loteActual().edad().diasTotales()).isEqualTo(17);
  }
}
