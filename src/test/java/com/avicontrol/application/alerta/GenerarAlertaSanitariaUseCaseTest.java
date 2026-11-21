package com.avicontrol.application.alerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.alerta.AlertaSanitaria;
import com.avicontrol.domain.model.mortalidad.NivelSeveridadMortalidad;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import com.avicontrol.domain.port.out.alerta.AlertaSanitariaRepositoryPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class GenerarAlertaSanitariaUseCaseTest {
  @Mock AlertaSanitariaRepositoryPort alertas;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));
  private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();

  @BeforeEach
  void initSync() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void clearSync() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private RegistroMortalidadAnormal reporte() {
    Instant ahora = Instant.now(clock);
    return new RegistroMortalidadAnormal(
        UUID.randomUUID(),
        null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        ahora.minusSeconds(60),
        ahora,
        30,
        1000,
        new BigDecimal("3.00"),
        NivelSeveridadMortalidad.CRITICA,
        "Mortalidad súbita",
        "Obs",
        "trabajador");
  }

  @Test
  void generaNuevaAlertaYEvitaDuplicadoPorReporte() {
    GenerarAlertaSanitariaUseCase useCase =
        new GenerarAlertaSanitariaUseCase(alertas, clock, metrics);
    RegistroMortalidadAnormal r = reporte();

    when(alertas.porReporte(r.id())).thenReturn(Optional.empty());
    UUID idGenerada = useCase.generar(r);
    assertThat(idGenerada).isNotNull();
    verify(alertas).guardar(any(AlertaSanitaria.class));

    UUID existenteId = UUID.randomUUID();
    when(alertas.porReporte(r.id())).thenReturn(Optional.of(existenteId));
    assertThat(useCase.generar(r)).isEqualTo(existenteId);
    verify(alertas, never()).guardar(argThatId(existenteId));
  }

  @Test
  void rechazaReporteNulo() {
    GenerarAlertaSanitariaUseCase useCase =
        new GenerarAlertaSanitariaUseCase(alertas, clock, metrics);
    assertThatThrownBy(() -> useCase.generar(null)).isInstanceOf(IllegalArgumentException.class);
  }

  private static AlertaSanitaria argThatId(UUID id) {
    return org.mockito.ArgumentMatchers.argThat(a -> a.id().equals(id));
  }
}
