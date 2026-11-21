package com.avicontrol.application.mortalidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.model.mortalidad.NivelSeveridadMortalidad;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.mortalidad.GenerarAlertaSanitariaPort;
import com.avicontrol.domain.port.out.mortalidad.RegistroMortalidadAnormalRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrarMortalidadAnormalUseCaseTest {
  @Mock GalponLoteAislamientoQueryPort galpones;
  @Mock LoteQueryPort lotes;
  @Mock RegistroMortalidadAnormalRepositoryPort reportes;
  @Mock GenerarAlertaSanitariaPort alertas;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void registraReporteAnormalYDisparaAlertaSanitaria() {
    RegistrarMortalidadAnormalUseCase useCase =
        new RegistrarMortalidadAnormalUseCase(galpones, lotes, reportes, alertas, clock);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    UUID alertaId = UUID.randomUUID();
    Instant evento = Instant.now(clock).minusSeconds(60);

    var cmd =
        new RegistrarMortalidadAnormalCommand(
            galponId, evento, 30, "Signos respiratorios", "Obs", "trabajador", key);

    when(reportes.buscar(key)).thenReturn(Optional.empty());
    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galponId, loteId, "PRODUCTIVA", 1000, LocalDate.of(2026, 11, 10))));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 1000, LocalDate.of(2026, 11, 10))));
    when(alertas.generar(any(RegistroMortalidadAnormal.class))).thenReturn(alertaId);

    var result = useCase.registrar(cmd);

    assertThat(result.alertaId()).isEqualTo(alertaId);
    assertThat(result.severidad()).isEqualTo("CRITICA");
    verify(reportes).bloquearGalpon(galponId);
    verify(reportes).guardar(any(RegistroMortalidadAnormal.class));
  }

  @Test
  void rechazaIdempotenciaConPayloadDistinto() {
    RegistrarMortalidadAnormalUseCase useCase =
        new RegistrarMortalidadAnormalUseCase(galpones, lotes, reportes, alertas, clock);
    UUID galponId = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    Instant evento = Instant.now(clock).minusSeconds(60);

    RegistroMortalidadAnormal previo =
        new RegistroMortalidadAnormal(
            UUID.randomUUID(),
            null,
            galponId,
            UUID.randomUUID(),
            evento,
            Instant.now(clock),
            20,
            1000,
            new BigDecimal("2.00"),
            NivelSeveridadMortalidad.ALTA,
            "Causa A",
            null,
            "trabajador");
    when(reportes.buscar(key)).thenReturn(Optional.of(previo));

    var cmdDistinto =
        new RegistrarMortalidadAnormalCommand(
            galponId, evento, 25, "Causa A", null, "trabajador", key);

    assertThatThrownBy(() -> useCase.registrar(cmdDistinto))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("idempotencia");
  }
}
