package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.ValidacionAptitudResult;
import com.avicontrol.domain.exception.galpon.GalponNoEncontradoException;
import com.avicontrol.domain.exception.sacrificio.productivo.GalponNoAptoParaSacrificioException;
import com.avicontrol.domain.exception.sacrificio.productivo.InformacionAptitudNoDisponibleException;
import com.avicontrol.domain.exception.sacrificio.productivo.LoteActualNoEncontradoException;
import com.avicontrol.domain.model.lote.Edad;
import com.avicontrol.domain.model.sacrificio.productivo.ValidacionAptitudSacrificio;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;

public class ValidarAptitudSacrificioUseCase {
  private final GalponQueryPort galpones;
  private final LoteQueryPort lotes;
  private final GalponUpdatePort cambios;
  private final Clock clock;

  public ValidarAptitudSacrificioUseCase(
      GalponQueryPort galpones, LoteQueryPort lotes, GalponUpdatePort cambios, Clock clock) {
    this.galpones = galpones;
    this.lotes = lotes;
    this.cambios = cambios;
    this.clock = clock;
  }

  @Transactional
  public ValidacionAptitudResult validar(ValidarAptitudSacrificioCommand comando) {
    if (comando == null || comando.galponId() == null)
      throw new IllegalArgumentException("galponId es obligatorio");
    var galpon =
        galpones.consultar(comando.galponId()).orElseThrow(GalponNoEncontradoException::new);
    var lote =
        lotes.consultarActual(comando.galponId()).orElseThrow(LoteActualNoEncontradoException::new);
    if (!lote.galponId().equals(galpon.id()) || lote.fechaIngreso() == null)
      throw new InformacionAptitudNoDisponibleException();
    Edad edad = lote.calcularEdad(LocalDate.now(clock));
    var validacion = ValidacionAptitudSacrificio.evaluar(galpon.estado(), edad);
    if (validacion.apto() && !cambios.pasarACosechaSiProductiva(galpon.id(), lote.id()))
      throw new GalponNoAptoParaSacrificioException();
    return new ValidacionAptitudResult(
        galpon.id(),
        lote.id(),
        validacion.apto(),
        validacion.motivosNoAptitud(),
        validacion.apto() ? "EN_COSECHA" : galpon.estado());
  }
}
