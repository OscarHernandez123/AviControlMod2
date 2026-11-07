package com.avicontrol.application.sanitario.medicacion;

import com.avicontrol.application.sanitario.medicacion.result.MedicacionDiariaGalponResult;
import com.avicontrol.domain.model.sanitario.medicacion.MedicacionDiaria;
import com.avicontrol.domain.model.sanitario.medicacion.ProgresoTratamiento;
import com.avicontrol.domain.port.out.sanitario.medicacion.DiagnosticoTratamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ConsultarMedicacionDiariaGalponUseCase {
  private final GalponLoteQueryPort galpones;
  private final DiagnosticoTratamientoQueryPort diagnosticos;
  private final Clock clock;

  public ConsultarMedicacionDiariaGalponUseCase(
      GalponLoteQueryPort galpones, DiagnosticoTratamientoQueryPort diagnosticos, Clock clock) {
    this.galpones = galpones;
    this.diagnosticos = diagnosticos;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public MedicacionDiariaGalponResult consultar(UUID galponId, LocalDate fechaSolicitada) {
    if (galponId == null) throw new IllegalArgumentException("galponId obligatorio");
    LocalDate fecha = fechaSolicitada == null ? LocalDate.now(clock) : fechaSolicitada;
    var galpon =
        galpones
            .consultar(galponId)
            .orElseThrow(() -> new NoSuchElementException("Galpón no encontrado"));
    List<MedicacionDiaria> tratamientos =
        galpon.loteId() == null || galpon.poblacionViva() <= 0
            ? List.of()
            : diagnosticos.consultar(galpon.loteId(), fecha).stream()
                .flatMap(
                    d ->
                        ProgresoTratamiento.calcular(d.fechaInicio(), d.duracionDias(), fecha)
                            .stream()
                            .map(
                                progreso ->
                                    new MedicacionDiaria(
                                        d.diagnosticoId(),
                                        galpon.loteCodigo(),
                                        progreso,
                                        d.enfermedad(),
                                        d.medicamento(),
                                        d.principioActivo(),
                                        d.dosis(),
                                        d.viaAdministracion(),
                                        d.aplicaHoy())))
                .toList();
    boolean activo = !tratamientos.isEmpty();
    return new MedicacionDiariaGalponResult(
        galpon.galponId(),
        galpon.nombre(),
        galpon.aforoMaximo(),
        galpon.estado(),
        galpon.loteId(),
        galpon.loteCodigo(),
        galpon.loteId() == null ? null : galpon.poblacionViva(),
        galpon.loteId() == null
            ? null
            : Math.toIntExact(ChronoUnit.DAYS.between(galpon.fechaIngreso(), fecha)),
        fecha,
        activo ? "CON_MEDICACION_ACTIVA" : "SIN_MEDICACION_ACTIVA",
        activo ? null : "Sin medicación activa para hoy",
        tratamientos);
  }
}
