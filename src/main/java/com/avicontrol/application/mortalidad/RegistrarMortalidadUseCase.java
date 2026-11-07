package com.avicontrol.application.mortalidad;

import com.avicontrol.application.modulo1.ActualizarInventarioVivoCommand;
import com.avicontrol.application.modulo1.ActualizarInventarioVivoUseCase;
import com.avicontrol.application.mortalidad.result.RegistroMortalidadResult;
import com.avicontrol.domain.model.mortalidad.ClasificacionMortalidad;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidad;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.mortalidad.RegistroMortalidadRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarMortalidadUseCase {
  private final GalponLoteAislamientoQueryPort galpones;
  private final LoteQueryPort lotes;
  private final ActualizarInventarioVivoUseCase inventario;
  private final RegistroMortalidadRepositoryPort registros;
  private final RegistrarMortalidadAnormalUseCase anormal;
  private final Clock clock;

  public RegistrarMortalidadUseCase(
      GalponLoteAislamientoQueryPort galpones,
      LoteQueryPort lotes,
      ActualizarInventarioVivoUseCase inventario,
      RegistroMortalidadRepositoryPort registros,
      RegistrarMortalidadAnormalUseCase anormal,
      Clock clock) {
    this.galpones = galpones;
    this.lotes = lotes;
    this.inventario = inventario;
    this.registros = registros;
    this.anormal = anormal;
    this.clock = clock;
  }

  @Transactional
  public RegistroMortalidadResult registrar(RegistrarMortalidadCommand c) {
    if (c == null
        || c.galponId() == null
        || c.fechaHoraEvento() == null
        || c.cantidad() <= 0
        || c.causa() == null
        || c.responsable() == null
        || c.responsable().isBlank()
        || c.idempotencyKey() == null) throw new IllegalArgumentException("Mortalidad inválida");
    var anterior = registros.buscar(c.idempotencyKey());
    if (anterior.isPresent()) return repetido(c, anterior.get());
    var galpon =
        galpones
            .consultar(c.galponId())
            .orElseThrow(() -> new NoSuchElementException("Galpón no encontrado"));
    var lote =
        lotes
            .consultarActual(c.galponId())
            .orElseThrow(() -> new NoSuchElementException("Lote activo no encontrado"));
    Instant ahora = Instant.now(clock);
    if (!"PRODUCTIVA".equals(galpon.estado()) || !lote.id().equals(galpon.loteId()))
      throw new IllegalStateException("Galpón no productivo o lote inconsistente");
    if (lote.fechaIngreso() == null
        || c.fechaHoraEvento().isAfter(ahora)
        || c.fechaHoraEvento().atZone(clock.getZone()).toLocalDate().isBefore(lote.fechaIngreso()))
      throw new IllegalArgumentException("Fecha de mortalidad inválida");
    if (c.cantidad() > lote.poblacionActual())
      throw new com.avicontrol.domain.exception.modulo1.PoblacionVivaInsuficienteException();
    var dia = c.fechaHoraEvento().atZone(clock.getZone()).toLocalDate();
    var inicio = dia.atStartOfDay(clock.getZone()).toInstant();
    var fin = dia.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
    int anteriores = registros.totalDia(c.galponId(), lote.id(), inicio, fin);
    var clasificacion =
        (anteriores + (long) c.cantidad()) * 1000L > lote.poblacionActual() + (long) anteriores
            ? ClasificacionMortalidad.ANORMAL
            : ClasificacionMortalidad.NORMAL;
    var descuento =
        inventario.actualizar(
            new ActualizarInventarioVivoCommand(
                lote.id(),
                c.galponId(),
                c.cantidad(),
                "MORTALIDAD",
                c.responsable(),
                -1,
                c.idempotencyKey()));
    var confirmado = registros.buscar(c.idempotencyKey());
    if (confirmado.isPresent()) return repetido(c, confirmado.get());
    var registro =
        new RegistroMortalidad(
            c.idempotencyKey(),
            c.galponId(),
            lote.id(),
            c.fechaHoraEvento(),
            ahora,
            c.cantidad(),
            c.causa(),
            c.observaciones(),
            c.responsable(),
            clasificacion,
            descuento.poblacionAnterior(),
            descuento.poblacionResultante());
    registros.guardar(registro);
    if (clasificacion == ClasificacionMortalidad.ANORMAL)
      anormal.desdeMortalidad(registro, lote.poblacionActual());
    return RegistroMortalidadResult.de(registro);
  }

  private RegistroMortalidadResult repetido(RegistrarMortalidadCommand c, RegistroMortalidad r) {
    if (!r.galponId().equals(c.galponId())
        || !r.fechaHoraEvento()
            .truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
            .equals(c.fechaHoraEvento().truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
        || r.cantidad() != c.cantidad()
        || r.causa() != c.causa()
        || !r.responsable().equals(c.responsable())
        || !java.util.Objects.equals(r.observaciones(), c.observaciones()))
      throw new IllegalStateException("Clave de idempotencia usada con otra mortalidad");
    return RegistroMortalidadResult.de(r);
  }
}
