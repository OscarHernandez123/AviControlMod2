package com.avicontrol.application.sanitario.aislamiento;

import com.avicontrol.application.sanitario.aislamiento.result.SolicitudAislamientoResult;
import com.avicontrol.domain.exception.sanitario.aislamiento.EstadoGalponNoAislableException;
import com.avicontrol.domain.exception.sanitario.aislamiento.GalponNoEncontradoException;
import com.avicontrol.domain.exception.sanitario.aislamiento.LoteActivoNoEncontradoException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoDuplicadaException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoInvalidaException;
import com.avicontrol.domain.model.sanitario.aislamiento.EstadoSolicitudAislamiento;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.model.sanitario.aislamiento.SolicitudAislamiento;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class CrearSolicitudAislamientoUseCase {
  private final SolicitudAislamientoRepositoryPort solicitudes;
  private final GalponLoteAislamientoQueryPort galpones;
  private final Clock clock;

  public CrearSolicitudAislamientoUseCase(
      SolicitudAislamientoRepositoryPort solicitudes,
      GalponLoteAislamientoQueryPort galpones,
      Clock clock) {
    this.solicitudes = solicitudes;
    this.galpones = galpones;
    this.clock = clock;
  }

  @Transactional
  public SolicitudAislamientoResult crear(CrearSolicitudAislamientoCommand command) {
    if (command == null || command.galponId() == null)
      throw new SolicitudAislamientoInvalidaException("Galpón obligatorio");
    GalponLoteAislamiento contexto =
        galpones.consultar(command.galponId()).orElseThrow(GalponNoEncontradoException::new);
    if (contexto.loteId() == null || contexto.fechaIngreso() == null)
      throw new LoteActivoNoEncontradoException();
    if (!"PRODUCTIVA".equals(contexto.estado()))
      throw new EstadoGalponNoAislableException("El galpón debe estar en productiva");
    if (contexto.poblacionViva() <= 0)
      throw new EstadoGalponNoAislableException("El lote no tiene aves vivas");
    LocalDate hoy = LocalDate.now(clock);
    long edad = ChronoUnit.DAYS.between(contexto.fechaIngreso(), hoy) + 1;
    if (edad <= 0 || edad > Integer.MAX_VALUE)
      throw new SolicitudAislamientoInvalidaException("Fecha de ingreso del lote inválida");
    if (solicitudes.existePendiente(command.galponId()))
      throw new SolicitudAislamientoDuplicadaException();
    SolicitudAislamiento solicitud =
        new SolicitudAislamiento(
            UUID.randomUUID(),
            command.galponId(),
            contexto.loteId(),
            command.motivo(),
            command.signosClinicos(),
            contexto.poblacionViva(),
            (int) edad,
            command.trabajador(),
            Instant.now(clock),
            EstadoSolicitudAislamiento.PENDIENTE_VALIDACION,
            null,
            null,
            null,
            0);
    solicitudes.crear(solicitud);
    return SolicitudAislamientoResult.de(solicitud);
  }
}
