package com.avicontrol.application.mortalidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.application.modulo1.ActualizarInventarioVivoCommand;
import com.avicontrol.application.modulo1.ActualizarInventarioVivoUseCase;
import com.avicontrol.application.modulo1.result.InventarioVivoActualizadoResult;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.model.mortalidad.CausaMortalidad;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidad;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.mortalidad.RegistroMortalidadRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
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
class RegistrarMortalidadUseCaseTest {
  @Mock GalponLoteAislamientoQueryPort galpones;
  @Mock LoteQueryPort lotes;
  @Mock ActualizarInventarioVivoUseCase inventario;
  @Mock RegistroMortalidadRepositoryPort registros;
  @Mock RegistrarMortalidadAnormalUseCase anormal;

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

  @Test
  void registraMortalidadNormalYDescuentaInventarioVivo() {
    RegistrarMortalidadUseCase useCase =
        new RegistrarMortalidadUseCase(
            galpones, lotes, inventario, registros, anormal, clock, metrics);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    Instant evento = Instant.now(clock).minusSeconds(120);
    var cmd =
        new RegistrarMortalidadCommand(
            galponId, evento, 1, CausaMortalidad.ACCIDENTE, "Obs", "trabajador", key);

    when(registros.buscar(key)).thenReturn(Optional.empty());
    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galponId, loteId, "PRODUCTIVA", 1000, LocalDate.of(2026, 11, 10))));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 1000, LocalDate.of(2026, 11, 10))));
    when(registros.totalDia(any(), any(), any(), any())).thenReturn(0);
    when(inventario.actualizar(any(ActualizarInventarioVivoCommand.class)))
        .thenReturn(new InventarioVivoActualizadoResult(loteId, 1000, -1, 999, "MORTALIDAD"));

    var result = useCase.registrar(cmd);

    assertThat(result.cantidad()).isEqualTo(1);
    assertThat(result.clasificacion()).isEqualTo("NORMAL");
    assertThat(result.poblacionResultante()).isEqualTo(999);
    verify(registros).guardar(any(RegistroMortalidad.class));
    verifyNoInteractions(anormal);
  }

  @Test
  void rechazaGalponInexistenteOCantidadInvalida() {
    RegistrarMortalidadUseCase useCase =
        new RegistrarMortalidadUseCase(
            galpones, lotes, inventario, registros, anormal, clock, metrics);
    UUID galponId = UUID.randomUUID();
    UUID key = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                useCase.registrar(
                    new RegistrarMortalidadCommand(
                        galponId, Instant.now(clock), 0, CausaMortalidad.OTRA, null, "trabajador", key)))
        .isInstanceOf(IllegalArgumentException.class);

    when(registros.buscar(key)).thenReturn(Optional.empty());
    when(galpones.consultar(galponId)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                useCase.registrar(
                    new RegistrarMortalidadCommand(
                        galponId,
                        Instant.now(clock),
                        2,
                        CausaMortalidad.OTRA,
                        null,
                        "trabajador",
                        key)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
