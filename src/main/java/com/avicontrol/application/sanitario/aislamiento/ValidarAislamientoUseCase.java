package com.avicontrol.application.sanitario.aislamiento;

import com.avicontrol.application.sanitario.aislamiento.result.ValidacionAislamientoResult;
import com.avicontrol.domain.exception.sanitario.aislamiento.EstadoGalponNoAislableException;
import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoInvalidaException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoNoEncontradaException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoNoPendienteException;
import com.avicontrol.domain.model.sanitario.aislamiento.DecisionAislamiento;
import com.avicontrol.domain.model.sanitario.aislamiento.EstadoSolicitudAislamiento;
import com.avicontrol.domain.model.sanitario.aislamiento.SolicitudAislamiento;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

public class ValidarAislamientoUseCase {
  private final SolicitudAislamientoRepositoryPort solicitudes;
  private final GalponQueryPort galpones;
  private final GalponUpdatePort actualizacion;
  private final Clock clock;

  public ValidarAislamientoUseCase(
      SolicitudAislamientoRepositoryPort solicitudes,
      GalponQueryPort galpones,
      GalponUpdatePort actualizacion,
      Clock clock) {
    this.solicitudes = solicitudes;
    this.galpones = galpones;
    this.actualizacion = actualizacion;
    this.clock = clock;
  }

  @Transactional
  public ValidacionAislamientoResult validar(ValidarAislamientoCommand command) {
    if (command == null || command.solicitudId() == null || command.decision() == null)
      throw new SolicitudAislamientoInvalidaException("Solicitud y decisión obligatorias");
    SolicitudAislamiento pendiente =
        solicitudes
            .buscar(command.solicitudId())
            .orElseThrow(SolicitudAislamientoNoEncontradaException::new);
    if (pendiente.estado() != EstadoSolicitudAislamiento.PENDIENTE_VALIDACION)
      throw new SolicitudAislamientoNoPendienteException();
    String estado =
        galpones.estadoVigente(pendiente.galponId()).orElseThrow(Modulo1NoDisponibleException::new);
    SolicitudAislamiento resuelta =
        pendiente.decidir(
            command.decision(), command.observaciones(), command.veterinario(), Instant.now(clock));
    if (command.decision() == DecisionAislamiento.ACEPTAR) {
      if (!"PRODUCTIVA".equals(estado))
        throw new EstadoGalponNoAislableException("El galpón ya no está en productiva");
      if (!actualizacion.aislarSiProductiva(pendiente.galponId()))
        throw new EstadoGalponNoAislableException(
            "El galpón cambió de estado durante la validación");
      estado = "AISLAMIENTO";
    }
    if (!solicitudes.resolver(pendiente, resuelta))
      throw new SolicitudAislamientoNoPendienteException();
    return new ValidacionAislamientoResult(
        resuelta.id(), resuelta.estado(), resuelta.galponId(), estado);
  }
}
