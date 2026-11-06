package com.avicontrol.application.alimentacion;

import com.avicontrol.application.alimentacion.port.ProyeccionAlimentoRepositoryPort;
import com.avicontrol.domain.model.nutricion.PlanNutricionalGalpon;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalGalponRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyeccionAlimentoUseCase {
  private final ProyeccionAlimentoRepositoryPort repositorio;
  private final PlanNutricionalGalponRepositoryPort planes;
  private final Clock clock;

  public ProyeccionAlimentoUseCase(
      ProyeccionAlimentoRepositoryPort repositorio,
      PlanNutricionalGalponRepositoryPort planes,
      Clock clock) {
    this.repositorio = repositorio;
    this.planes = planes;
    this.clock = clock;
  }

  @Transactional
  public void activar(PlanNutricionalGalpon plan, PlanNutricionalGalpon.Etapa etapa) {
    activar(plan, etapa, false);
  }

  @Transactional
  public void activarHistorica(PlanNutricionalGalpon plan, PlanNutricionalGalpon.Etapa etapa) {
    activar(plan, etapa, true);
  }

  private void activar(
      PlanNutricionalGalpon plan,
      PlanNutricionalGalpon.Etapa etapa,
      boolean recuperadaHistoricamente) {
    repositorio.bloquearEtapa(etapa.id());
    if (repositorio.existe(etapa.id())) return;
    BigDecimal costo =
        recuperadaHistoricamente ? null : costoCapturado(etapa.alimentoId(), LocalDate.now(clock));
    BigDecimal kg =
        BigDecimal.valueOf(plan.poblacionViva())
            .multiply(etapa.racionKgAveDia())
            .multiply(BigDecimal.valueOf(etapa.dias()));
    repositorio.crear(
        new Proyeccion(
            UUID.randomUUID(),
            etapa.id(),
            plan.loteId(),
            plan.galponId(),
            etapa.alimentoId(),
            etapa.etapaCrianza().name(),
            etapa.tipoAlimento().name(),
            "ACTIVA",
            plan.poblacionViva(),
            etapa.racionKgAveDia(),
            etapa.pesoNominalPorBultoKg(),
            etapa.dias(),
            0,
            kg,
            costo,
            costo == null ? null : "PROMEDIO_PONDERADO_STOCK",
            costo == null ? null : Instant.now(clock),
            costo == null
                ? recuperadaHistoricamente
                    ? "Etapa recuperada después de su cierre; costo histórico no verificable"
                    : "No había saldo elegible al activar la etapa"
                : null,
            false,
            0));
  }

  @Transactional
  public void ajustar(
      UUID etapaId, int diasNuevos, String motivo, String justificacion, String actor) {
    Proyeccion anterior = repositorio.buscarPorEtapa(etapaId);
    if (anterior == null) throw new NoSuchElementException("Proyección no encontrada");
    if (!"ACTIVA".equals(anterior.estado()))
      throw new IllegalStateException("La proyección está cerrada");
    BigDecimal kgNuevo =
        BigDecimal.valueOf(anterior.poblacionInicioEtapa())
            .multiply(anterior.cuotaKgAveDia())
            .multiply(BigDecimal.valueOf(diasNuevos));
    if (!repositorio.actualizarDuracion(
        anterior.id(), anterior.version(), diasNuevos - anterior.diasBase(), kgNuevo))
      throw new IllegalStateException("La proyección cambió concurrentemente");
    repositorio.registrarAjuste(
        anterior.id(),
        new HistorialAjuste(
            Instant.now(clock),
            actor,
            motivo,
            justificacion,
            anterior.diasBase() + anterior.diasProrroga(),
            diasNuevos,
            anterior.proyeccionKg(),
            kgNuevo));
  }

  @Transactional
  public void cerrar(UUID etapaId) {
    repositorio.cerrar(etapaId);
  }

  @Transactional
  public Proyeccion completarCosto(UUID id, BigDecimal costo, String justificacion, String actor) {
    if (costo == null
        || costo.signum() <= 0
        || costo.scale() > 2
        || justificacion == null
        || justificacion.isBlank())
      throw new IllegalArgumentException("Costo y justificación válidos son obligatorios");
    if (repositorio.buscar(id) == null)
      throw new NoSuchElementException("Proyección no encontrada");
    Instant fecha = Instant.now(clock);
    if (!repositorio.completarCosto(id, costo, fecha))
      throw new IllegalStateException("El costo ya existe o la etapa está cerrada");
    repositorio.auditarCosto(id, costo, justificacion, actor, fecha);
    Proyeccion resultado = repositorio.buscar(id);
    if (resultado == null) throw new NoSuchElementException("Proyección no encontrada");
    return resultado;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Requerimiento consultarLote(UUID loteId) {
    PlanNutricionalGalpon plan =
        planes
            .buscarPorLote(loteId)
            .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));
    return new Requerimiento(
        plan.codigoLote(),
        loteId,
        plan.galponId(),
        "FINALIZADO".equals(plan.estado()) ? "FINALIZADO" : "EN_PROGRESO",
        Instant.now(clock),
        repositorio.listarPorLote(loteId));
  }

  public List<HistorialAjuste> historial(UUID proyeccionId) {
    return repositorio.historial(proyeccionId);
  }

  public String nombreAlimento(UUID alimentoId) {
    return repositorio.nombreAlimento(alimentoId);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<Balance> balance() {
    return repositorio.demandaActiva().stream()
        .map(
            d -> {
              BigDecimal stock =
                  repositorio.stock(
                      d.alimentoId(), d.pesoNominalPorBultoKg(), LocalDate.now(clock));
              BigDecimal diferencia = stock.subtract(d.demandaConsolidadaKg());
              BigDecimal bultos =
                  diferencia.signum() < 0
                      ? diferencia
                          .negate()
                          .divide(d.pesoNominalPorBultoKg(), 0, RoundingMode.CEILING)
                      : BigDecimal.ZERO;
              return new Balance(
                  d.alimentoId(),
                  d.pesoNominalPorBultoKg(),
                  d.demandaConsolidadaKg(),
                  stock,
                  diferencia,
                  diferencia.signum() < 0 ? "REABASTECIMIENTO_NECESARIO" : "CUBIERTO",
                  bultos);
            })
        .toList();
  }

  public BigDecimal demandaVigente(UUID alimentoId, String nombreNormalizado, String tipoAlimento) {
    return repositorio.demandaVigente(alimentoId, nombreNormalizado, tipoAlimento);
  }

  private BigDecimal costoCapturado(UUID alimentoId, LocalDate fecha) {
    if (alimentoId == null) return null;
    BigDecimal cantidad = BigDecimal.ZERO;
    BigDecimal total = BigDecimal.ZERO;
    for (var saldo : repositorio.saldosElegibles(alimentoId, fecha)) {
      if (saldo.saldo().signum() > 0) {
        cantidad = cantidad.add(saldo.saldo());
        total = total.add(saldo.saldo().multiply(saldo.precio()));
      }
    }
    return cantidad.signum() == 0 ? null : total.divide(cantidad, 2, RoundingMode.HALF_UP);
  }

  public record Proyeccion(
      UUID id,
      UUID etapaId,
      UUID loteId,
      UUID galponId,
      UUID alimentoId,
      String etapaCrianza,
      String tipoAlimento,
      String estado,
      int poblacionInicioEtapa,
      BigDecimal cuotaKgAveDia,
      BigDecimal pesoNominalPorBultoKg,
      int diasBase,
      int diasProrroga,
      BigDecimal proyeccionKg,
      BigDecimal costoUnitarioKg,
      String fuenteCosto,
      Instant fechaCapturaCosto,
      String advertenciaCosto,
      boolean costoCompletadoManualmente,
      long version) {}

  public record Requerimiento(
      String codigoLote,
      UUID loteId,
      UUID galponId,
      String estadoCiclo,
      Instant fechaConsulta,
      List<Proyeccion> etapas) {}

  public record HistorialAjuste(
      Instant fechaHora,
      String usuario,
      String motivo,
      String justificacion,
      int diasAnteriores,
      int diasNuevos,
      BigDecimal kilogramosAnteriores,
      BigDecimal kilogramosNuevos) {}

  public record Balance(
      UUID alimentoId,
      BigDecimal pesoNominalPorBultoKg,
      BigDecimal demandaConsolidadaKg,
      BigDecimal existenciaDisponibleKg,
      BigDecimal balanceKg,
      String estado,
      BigDecimal bultosSugeridos) {}
}
