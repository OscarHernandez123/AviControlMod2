package com.avicontrol.application.alimentacion;

import com.avicontrol.domain.exception.alimentacion.AlimentoNoCorrespondeEtapaException;
import com.avicontrol.domain.exception.alimentacion.LoteActivoNoEncontradoException;
import com.avicontrol.domain.exception.alimentacion.SuministroDuplicadoException;
import com.avicontrol.domain.model.alimentacion.SuministroDiarioAlimento;
import com.avicontrol.domain.port.out.alimentacion.InventarioAlimentoPort;
import com.avicontrol.domain.port.out.alimentacion.PlanNutricionalQueryPort;
import com.avicontrol.domain.port.out.alimentacion.SuministroDiarioRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarSuministroDiarioUseCase {
  private final GalponLoteQueryPort galpones;
  private final PlanNutricionalQueryPort planes;
  private final InventarioAlimentoPort inventario;
  private final SuministroDiarioRepositoryPort suministros;
  private final Clock clock;

  public RegistrarSuministroDiarioUseCase(
      GalponLoteQueryPort galpones,
      PlanNutricionalQueryPort planes,
      InventarioAlimentoPort inventario,
      SuministroDiarioRepositoryPort suministros,
      Clock clock) {
    this.galpones = galpones;
    this.planes = planes;
    this.inventario = inventario;
    this.suministros = suministros;
    this.clock = clock;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Registro registrar(
      UUID galponId,
      BigDecimal cantidadKg,
      LocalDate fecha,
      UUID idempotenciaClave,
      String trabajador) {
    if (galponId == null
        || idempotenciaClave == null
        || cantidadKg == null
        || cantidadKg.signum() <= 0
        || cantidadKg.scale() > 4
        || fecha == null
        || trabajador == null
        || trabajador.isBlank())
      throw new IllegalArgumentException("Datos del suministro inválidos");
    var anterior = suministros.buscarPorClave(galponId, idempotenciaClave);
    if (anterior.isPresent())
      return validarReintento(anterior.orElseThrow(), cantidadKg, fecha, trabajador);
    if (!fecha.equals(LocalDate.now(clock)))
      throw new IllegalArgumentException("El suministro se registra para la jornada actual");
    var galpon =
        galpones
            .bloquear(galponId)
            .orElseThrow(() -> new NoSuchElementException("Galpón no encontrado"));
    anterior = suministros.buscarPorClave(galponId, idempotenciaClave);
    if (anterior.isPresent())
      return validarReintento(anterior.orElseThrow(), cantidadKg, fecha, trabajador);
    if (galpon.loteId() == null || galpon.poblacionViva() <= 0)
      throw new LoteActivoNoEncontradoException();
    int edad = Math.toIntExact(ChronoUnit.DAYS.between(galpon.fechaIngreso(), fecha) + 1);
    var etapa =
        planes
            .buscar(galpon.loteId(), fecha, edad)
            .orElseThrow(AlimentoNoCorrespondeEtapaException::new);
    if (etapa.alimentoId() == null) throw new AlimentoNoCorrespondeEtapaException();
    var detalles =
        inventario.reservarFefo(etapa.alimentoId(), etapa.tipoAlimento(), cantidadKg, fecha);
    var suministro =
        new SuministroDiarioAlimento(
            UUID.randomUUID(),
            galponId,
            galpon.loteId(),
            etapa.alimentoId(),
            etapa.tipoAlimento(),
            cantidadKg,
            fecha,
            Instant.now(clock),
            trabajador,
            idempotenciaClave,
            detalles);
    suministros.guardar(suministro);
    return new Registro(suministro, false);
  }

  private Registro validarReintento(
      SuministroDiarioAlimento anterior,
      BigDecimal cantidadKg,
      LocalDate fecha,
      String trabajador) {
    if (anterior.cantidadKg().compareTo(cantidadKg) != 0
        || !anterior.fechaAplicacion().equals(fecha)
        || !anterior.trabajador().equals(trabajador)) throw new SuministroDuplicadoException();
    return new Registro(anterior, true);
  }

  public record Registro(SuministroDiarioAlimento suministro, boolean repetido) {}
}
