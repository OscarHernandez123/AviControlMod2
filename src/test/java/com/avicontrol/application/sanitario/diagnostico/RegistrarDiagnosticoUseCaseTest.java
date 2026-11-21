package com.avicontrol.application.sanitario.diagnostico;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sanitario.diagnostico.GalponNoAisladoException;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.MedicacionQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarDiagnosticoUseCaseTest {
  @Test
  void reconsultaEstadoAntesDeGuardar() {
    UUID galpon = UUID.randomUUID();
    Enfermedad e =
        new Enfermedad(
            UUID.randomUUID(),
            "Newcastle",
            NivelRiesgo.ALTO,
            TipoEnfermedad.VIRAL,
            "Signos",
            true,
            0,
            Instant.now(),
            Instant.now());
    DiagnosticoGalponRepositoryPort repo = mock(DiagnosticoGalponRepositoryPort.class);
    EnfermedadQueryPort enfermedades = mock(EnfermedadQueryPort.class);
    MedicacionQueryPort medicaciones = mock(MedicacionQueryPort.class);
    GalponLoteAislamientoQueryPort galpones = mock(GalponLoteAislamientoQueryPort.class);
    when(enfermedades.buscarEnfermedad(e.id())).thenReturn(Optional.of(e));
    when(galpones.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galpon, UUID.randomUUID(), "PRODUCTIVA", 8000, LocalDate.now())));
    assertThrows(
        GalponNoAisladoException.class,
        () ->
            new RegistrarDiagnosticoUseCase(
                    repo, enfermedades, medicaciones, galpones, Clock.systemUTC())
                .registrar(new RegistrarDiagnosticoCommand(galpon, e.id(), null, "Signos graves")));
    verify(galpones).consultar(galpon);
    verify(repo, never()).crear(any());
  }
}
