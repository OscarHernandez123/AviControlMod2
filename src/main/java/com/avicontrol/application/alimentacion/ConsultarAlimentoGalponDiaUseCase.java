package com.avicontrol.application.alimentacion;

import com.avicontrol.application.alimentacion.result.AlimentoDiarioGalponResult;
import com.avicontrol.domain.model.alimentacion.AlertaAlimentacion;
import com.avicontrol.domain.model.alimentacion.RequerimientoDiarioGalpon;
import com.avicontrol.domain.port.out.alimentacion.InventarioAlimentoPort;
import com.avicontrol.domain.port.out.alimentacion.ObservabilidadAlimentacionPort;
import com.avicontrol.domain.port.out.alimentacion.PlanNutricionalQueryPort;
import com.avicontrol.domain.port.out.alimentacion.SuministroDiarioQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ConsultarAlimentoGalponDiaUseCase {
  private final GalponLoteQueryPort galpones;
  private final PlanNutricionalQueryPort planes;
  private final SuministroDiarioQueryPort suministros;
  private final InventarioAlimentoPort inventario;
  private final ObservabilidadAlimentacionPort observabilidad;
  private final Clock clock;

  public ConsultarAlimentoGalponDiaUseCase(
      GalponLoteQueryPort galpones,
      PlanNutricionalQueryPort planes,
      SuministroDiarioQueryPort suministros,
      InventarioAlimentoPort inventario,
      ObservabilidadAlimentacionPort observabilidad,
      Clock clock) {
    this.galpones = galpones;
    this.planes = planes;
    this.suministros = suministros;
    this.inventario = inventario;
    this.observabilidad = observabilidad;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public AlimentoDiarioGalponResult consultar(UUID galponId, LocalDate fechaSolicitada) {
    if (galponId == null) throw new IllegalArgumentException("galponId obligatorio");
    LocalDate fecha = fechaSolicitada == null ? LocalDate.now(clock) : fechaSolicitada;
    if (!fecha.equals(LocalDate.now(clock)))
      throw new IllegalArgumentException("La consulta usa la jornada actual");
    var galpon =
        galpones
            .consultar(galponId)
            .orElseThrow(() -> new NoSuchElementException("Galpón no encontrado"));
    Integer edad =
        galpon.loteId() == null || galpon.fechaIngreso() == null
            ? null
            : Math.toIntExact(ChronoUnit.DAYS.between(galpon.fechaIngreso(), fecha) + 1);
    if (galpon.loteId() == null || galpon.poblacionViva() <= 0 || edad == null || edad < 1)
      return sinCuota(
          galpon, fecha, edad, galpon.loteId() == null ? "SIN_LOTE_ACTIVO" : "SIN_AVES_VIVAS");
    var etapa = planes.buscar(galpon.loteId(), fecha, edad);
    if (etapa.isEmpty()) return sinCuota(galpon, fecha, edad, "SIN_ETAPA_NUTRICIONAL");
    var e = etapa.orElseThrow();
    BigDecimal suministrado =
        e.alimentoId() == null
            ? BigDecimal.ZERO
            : suministros.acumulado(galpon.loteId(), e.alimentoId(), fecha);
    BigDecimal disponible =
        e.alimentoId() == null
            ? BigDecimal.ZERO
            : inventario.disponible(e.alimentoId(), e.tipoAlimento(), fecha);
    var requerimiento =
        RequerimientoDiarioGalpon.calcular(
            galpon.poblacionViva(),
            e.racionKgAveDia(),
            e.pesoNominalBultoKg(),
            suministrado,
            disponible);
    List<AlertaAlimentacion> alertas = new ArrayList<>();
    if (requerimiento.requerimientoKg().compareTo(disponible) > 0) {
      alertas.add(
          new AlertaAlimentacion("STOCK_INSUFICIENTE", "La bodega no cubre la ración del día"));
      observabilidad.registrarDeficit();
    }
    if (requerimiento.saldoKg().signum() > 0)
      alertas.add(
          new AlertaAlimentacion(
              "SUMINISTRO_PENDIENTE",
              "Faltan " + requerimiento.saldoKg().stripTrailingZeros().toPlainString() + " kg"));
    if (e.cambioHoy())
      alertas.add(new AlertaAlimentacion("PLAN_ACTUALIZADO_HOY", "La etapa o dieta cambió hoy"));
    return new AlimentoDiarioGalponResult(
        galpon.galponId(),
        galpon.nombre(),
        galpon.aforoMaximo(),
        galpon.estado(),
        galpon.loteId(),
        galpon.loteCodigo(),
        galpon.poblacionViva(),
        edad,
        fecha,
        "CON_PLAN_NUTRICIONAL",
        e.etapaCrianza(),
        e.tipoAlimento(),
        e.alimentoId(),
        e.nombreAlimento(),
        e.pesoNominalBultoKg(),
        requerimiento,
        alertas);
  }

  private AlimentoDiarioGalponResult sinCuota(
      GalponLoteQueryPort.GalponLote galpon, LocalDate fecha, Integer edad, String estado) {
    var cero =
        new RequerimientoDiarioGalpon(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    return new AlimentoDiarioGalponResult(
        galpon.galponId(),
        galpon.nombre(),
        galpon.aforoMaximo(),
        galpon.estado(),
        galpon.loteId(),
        galpon.loteCodigo(),
        galpon.loteId() == null ? null : galpon.poblacionViva(),
        edad,
        fecha,
        estado,
        null,
        null,
        null,
        null,
        null,
        cero,
        List.of());
  }
}
