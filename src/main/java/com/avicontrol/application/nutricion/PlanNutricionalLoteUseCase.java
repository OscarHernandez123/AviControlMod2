package com.avicontrol.application.nutricion;

import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.AuditoriaPlanNutricional;
import com.avicontrol.domain.model.nutricion.PlanNutricionalGalpon;
import com.avicontrol.domain.model.nutricion.PlanNutricionalPlantilla;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalGalponRepositoryPort;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalPlantillaRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanNutricionalLoteUseCase {
  private final PlanNutricionalGalponRepositoryPort planes;
  private final PlanNutricionalPlantillaRepositoryPort plantillas;
  private final AlimentoCompatibleQueryPort alimentos;
  private final Clock clock;
  private final ProyeccionAlimentoUseCase proyecciones;
  private final ApplicationEventPublisher eventos;

  public PlanNutricionalLoteUseCase(
      PlanNutricionalGalponRepositoryPort planes,
      PlanNutricionalPlantillaRepositoryPort plantillas,
      AlimentoCompatibleQueryPort alimentos,
      Clock clock,
      ProyeccionAlimentoUseCase proyecciones,
      ApplicationEventPublisher eventos) {
    this.planes = planes;
    this.plantillas = plantillas;
    this.alimentos = alimentos;
    this.clock = clock;
    this.proyecciones = proyecciones;
    this.eventos = eventos;
  }

  /** Puerto de entrada para el evento confirmado de alojamiento del Módulo 1. */
  @Transactional
  public PlanNutricionalGalpon asignarAlAlojar(Alojamiento alojamiento) {
    if (alojamiento == null
        || alojamiento.galponId() == null
        || alojamiento.loteId() == null
        || alojamiento.fechaIngreso() == null
        || alojamiento.tipoAve() == null
        || alojamiento.edadIngresoDias() < 1
        || alojamiento.poblacionViva() < 0)
      throw new IllegalArgumentException("Datos del alojamiento inválidos");
    if (planes.buscarPorLote(alojamiento.loteId()).isPresent())
      return planes.buscarPorLote(alojamiento.loteId()).orElseThrow();
    PlanNutricionalPlantilla plantilla =
        plantillas.predeterminada(alojamiento.tipoAve()).orElse(null);
    PlanNutricionalGalpon nuevo = construir(alojamiento, plantilla);
    planes.crear(nuevo);
    if (plantilla == null)
      eventos.publishEvent(
          new PlanNutricionalPendienteEvent(
              nuevo.galponId(), nuevo.loteId(), nuevo.tipoAve(), "ALTA"));
    auditar(
        nuevo.id(),
        null,
        "ASIGNACION_AUTOMATICA",
        "SISTEMA",
        null,
        null,
        plantilla == null ? "PENDIENTE_ASIGNACION" : plantilla.id().toString());
    nuevo.etapas().stream()
        .filter(e -> "ACTIVA".equals(e.estado()))
        .forEach(e -> proyecciones.activar(nuevo, e));
    return nuevo;
  }

  @Transactional
  public PlanNutricionalGalpon asignarManual(
      UUID galponId, Alojamiento alojamiento, UUID plantillaId) {
    return asignarManual(galponId, alojamiento, plantillaId, "NUTRICIONISTA_LOCAL");
  }

  @Transactional
  public PlanNutricionalGalpon asignarManual(
      UUID galponId, Alojamiento alojamiento, UUID plantillaId, String actor) {
    if (galponId == null
        || alojamiento == null
        || alojamiento.galponId() == null
        || alojamiento.loteId() == null
        || alojamiento.fechaIngreso() == null
        || alojamiento.tipoAve() == null
        || alojamiento.edadIngresoDias() < 1
        || alojamiento.poblacionViva() < 0
        || plantillaId == null)
      throw new IllegalArgumentException("Datos del alojamiento o plantilla inválidos");
    if (!galponId.equals(alojamiento.galponId()))
      throw new IllegalArgumentException("El galpón no corresponde al alojamiento");
    PlanNutricionalPlantilla plantilla = plantilla(plantillaId);
    if (!plantilla.tipoAve().equals(alojamiento.tipoAve()))
      throw new IllegalArgumentException("La plantilla no corresponde al tipo de ave");
    PlanNutricionalGalpon existente = planes.buscarPorLote(alojamiento.loteId()).orElse(null);
    if (existente != null && !"PENDIENTE_ASIGNACION".equals(existente.estado()))
      throw new IllegalStateException("El lote ya tiene un plan; use sustitución");
    PlanNutricionalGalpon asignado = construir(alojamiento, plantilla);
    if (existente == null) planes.crear(asignado);
    else
      planes.actualizar(
          new PlanNutricionalGalpon(
              existente.id(),
              asignado.galponId(),
              asignado.loteId(),
              asignado.codigoLote(),
              plantilla.id(),
              asignado.tipoAve(),
              asignado.fechaIngreso(),
              asignado.edadIngresoDias(),
              asignado.poblacionViva(),
              asignado.fechaSalidaProyectada(),
              "ACTIVO",
              asignado.etapas()));
    PlanNutricionalGalpon resultado = planes.buscarPorLote(alojamiento.loteId()).orElseThrow();
    auditar(
        resultado.id(),
        null,
        "ASIGNACION_MANUAL",
        actor,
        null,
        existente == null ? null : "PENDIENTE_ASIGNACION",
        plantillaId.toString());
    resultado.etapas().stream()
        .filter(e -> "ACTIVA".equals(e.estado()))
        .forEach(e -> proyecciones.activar(resultado, e));
    return resultado;
  }

  @Transactional
  public PlanNutricionalGalpon sustituir(UUID galponId, UUID plantillaId, String justificacion) {
    return sustituir(galponId, plantillaId, justificacion, "NUTRICIONISTA_LOCAL");
  }

  @Transactional
  public PlanNutricionalGalpon sustituir(
      UUID galponId, UUID plantillaId, String justificacion, String actor) {
    if (justificacion == null || justificacion.isBlank())
      throw new IllegalArgumentException("La justificación es obligatoria");
    PlanNutricionalGalpon actual = plan(galponId);
    PlanNutricionalPlantilla nueva = plantilla(plantillaId);
    if (!nueva.tipoAve().equals(actual.tipoAve()))
      throw new IllegalArgumentException("La plantilla no corresponde al tipo de ave");
    List<PlanNutricionalGalpon.Etapa> etapas = new ArrayList<>();
    int siguiente = 1;
    for (PlanNutricionalGalpon.Etapa anterior : actual.etapas()) {
      PlanNutricionalPlantilla.Etapa propuesta =
          nueva.etapas().stream()
              .filter(e -> e.etapaCrianza() == anterior.etapaCrianza())
              .findFirst()
              .orElseThrow();
      if (!"FUTURA".equals(anterior.estado())) {
        etapas.add(anterior);
        siguiente = anterior.diaFin() + 1;
      } else {
        int fin = siguiente + propuesta.diaFin() - propuesta.diaInicio();
        etapas.add(
            new PlanNutricionalGalpon.Etapa(
                anterior.id(),
                anterior.etapaCrianza(),
                "FUTURA",
                siguiente,
                fin,
                propuesta.racionKgAveDia(),
                propuesta.tipoAlimento(),
                propuesta.alimentoId(),
                propuesta.pesoNominalPorBultoKg()));
        siguiente = fin + 1;
      }
    }
    PlanNutricionalGalpon actualizado = nuevoPlan(actual, plantillaId, etapas);
    planes.actualizar(actualizado);
    notificarSalida(actual, actualizado);
    auditar(
        actualizado.id(),
        null,
        "SUSTITUCION",
        actor,
        justificacion,
        actual.plantillaId() == null ? null : actual.plantillaId().toString(),
        plantillaId.toString());
    return actualizado;
  }

  @Transactional
  public PlanNutricionalGalpon seleccionarAlimento(UUID galponId, UUID etapaId, UUID alimentoId) {
    return seleccionarAlimento(galponId, etapaId, alimentoId, "NUTRICIONISTA_LOCAL");
  }

  @Transactional
  public PlanNutricionalGalpon seleccionarAlimento(
      UUID galponId, UUID etapaId, UUID alimentoId, String actor) {
    PlanNutricionalGalpon actual = plan(galponId);
    UUID alimentoAnterior =
        actual.etapas().stream()
            .filter(e -> e.id().equals(etapaId))
            .findFirst()
            .map(PlanNutricionalGalpon.Etapa::alimentoId)
            .orElse(null);
    AlimentoCompatibleQueryPort.Alimento alimento =
        alimentos
            .buscar(alimentoId)
            .orElseThrow(() -> new NoSuchElementException("Alimento no encontrado"));
    List<PlanNutricionalGalpon.Etapa> etapas = new ArrayList<>();
    int siguiente = 1;
    boolean encontrada = false;
    for (PlanNutricionalGalpon.Etapa e : actual.etapas()) {
      if (e.id().equals(etapaId)) {
        encontrada = true;
        if (!"FUTURA".equals(e.estado()))
          throw new IllegalStateException("Solo puede cambiarse el alimento de una etapa futura");
        if (e.tipoAlimento() != alimento.tipoAlimento())
          throw new IllegalArgumentException("Alimento incompatible con la etapa");
        int dias =
            alimento.duracionRecomendadaEtapaDias() == null
                ? e.dias()
                : alimento.duracionRecomendadaEtapaDias();
        if (dias < 1) throw new IllegalArgumentException("Duración recomendada inválida");
        e =
            new PlanNutricionalGalpon.Etapa(
                e.id(),
                e.etapaCrianza(),
                e.estado(),
                e.diaInicio(),
                e.diaInicio() + dias - 1,
                e.racionKgAveDia(),
                e.tipoAlimento(),
                alimento.id(),
                alimento.pesoNominalPorBultoKg());
      } else if ("FUTURA".equals(e.estado()) && siguiente != e.diaInicio()) {
        e =
            new PlanNutricionalGalpon.Etapa(
                e.id(),
                e.etapaCrianza(),
                e.estado(),
                siguiente,
                siguiente + e.dias() - 1,
                e.racionKgAveDia(),
                e.tipoAlimento(),
                e.alimentoId(),
                e.pesoNominalPorBultoKg());
      }
      etapas.add(e);
      siguiente = e.diaFin() + 1;
    }
    if (!encontrada) throw new NoSuchElementException("Etapa no encontrada");
    PlanNutricionalGalpon actualizado = nuevoPlan(actual, actual.plantillaId(), etapas);
    planes.actualizar(actualizado);
    notificarSalida(actual, actualizado);
    auditar(
        actualizado.id(),
        etapaId,
        "SELECCION_ALIMENTO",
        actor,
        null,
        alimentoAnterior == null ? null : alimentoAnterior.toString(),
        alimentoId.toString());
    return actualizado;
  }

  @Transactional
  public PlanNutricionalGalpon ajustarDuracion(
      UUID galponId,
      UUID etapaId,
      int nuevoDiaFin,
      String motivo,
      String justificacion,
      String actor) {
    if (!List.of("CUARENTENA_SANITARIA", "BAJO_PESO").contains(motivo)
        || justificacion == null
        || justificacion.isBlank())
      throw new IllegalArgumentException("Motivo y justificación técnica son obligatorios");
    PlanNutricionalGalpon actual = plan(galponId);
    List<PlanNutricionalGalpon.Etapa> etapas = new ArrayList<>();
    int desplazamiento = 0;
    PlanNutricionalGalpon.Etapa objetivo = null;
    for (PlanNutricionalGalpon.Etapa e : actual.etapas()) {
      if (e.id().equals(etapaId)) {
        if (!"ACTIVA".equals(e.estado())
            || nuevoDiaFin < e.diaInicio()
            || nuevoDiaFin < edadActual(actual))
          throw new IllegalStateException("Ajuste no permitido para la etapa");
        objetivo = e;
        desplazamiento = nuevoDiaFin - e.diaFin();
        e =
            new PlanNutricionalGalpon.Etapa(
                e.id(),
                e.etapaCrianza(),
                e.estado(),
                e.diaInicio(),
                nuevoDiaFin,
                e.racionKgAveDia(),
                e.tipoAlimento(),
                e.alimentoId(),
                e.pesoNominalPorBultoKg());
      } else if (desplazamiento != 0 && "FUTURA".equals(e.estado())) {
        e =
            new PlanNutricionalGalpon.Etapa(
                e.id(),
                e.etapaCrianza(),
                e.estado(),
                e.diaInicio() + desplazamiento,
                e.diaFin() + desplazamiento,
                e.racionKgAveDia(),
                e.tipoAlimento(),
                e.alimentoId(),
                e.pesoNominalPorBultoKg());
      }
      etapas.add(e);
    }
    if (objetivo == null) throw new NoSuchElementException("Etapa no encontrada");
    PlanNutricionalGalpon actualizado = nuevoPlan(actual, actual.plantillaId(), etapas);
    planes.actualizar(actualizado);
    notificarSalida(actual, actualizado);
    planes.registrarAjuste(
        etapaId, actor, motivo, justificacion, objetivo.diaFin(), nuevoDiaFin, Instant.now(clock));
    proyecciones.ajustar(
        etapaId, nuevoDiaFin - objetivo.diaInicio() + 1, motivo, justificacion, actor);
    auditar(
        actualizado.id(),
        etapaId,
        "AJUSTE_DURACION",
        actor,
        justificacion,
        String.valueOf(objetivo.diaFin()),
        String.valueOf(nuevoDiaFin));
    return actualizado;
  }

  public List<AuditoriaPlanNutricional> historial(UUID planId) {
    return planes.historial(planId);
  }

  private void auditar(
      UUID planId,
      UUID etapaId,
      String accion,
      String actor,
      String justificacion,
      String anterior,
      String nuevo) {
    planes.registrarAuditoria(
        planId,
        new AuditoriaPlanNutricional(
            etapaId, accion, actor, justificacion, anterior, nuevo, Instant.now(clock)));
  }

  private void notificarSalida(PlanNutricionalGalpon antes, PlanNutricionalGalpon despues) {
    if (!antes.fechaSalidaProyectada().equals(despues.fechaSalidaProyectada()))
      eventos.publishEvent(
          new FechaSalidaProyectadaActualizadaEvent(
              despues.galponId(),
              despues.loteId(),
              antes.fechaSalidaProyectada(),
              despues.fechaSalidaProyectada()));
  }

  public PlanNutricionalGalpon consultar(UUID galponId) {
    return plan(galponId);
  }

  /**
   * El adaptador del Módulo 1 puede entregar la población viva al confirmar cada cambio de etapa.
   */
  @Transactional
  public PlanNutricionalGalpon sincronizarEtapas(UUID loteId, int poblacionViva, LocalDate fecha) {
    if (poblacionViva < 0 || fecha == null)
      throw new IllegalArgumentException("Datos de lote inválidos");
    PlanNutricionalGalpon actual =
        planes
            .buscarPorLote(loteId)
            .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));
    if (!"ACTIVO".equals(actual.estado())) return actual;
    int edad =
        actual.edadIngresoDias()
            + (int) java.time.temporal.ChronoUnit.DAYS.between(actual.fechaIngreso(), fecha);
    List<PlanNutricionalGalpon.Etapa> etapas = new ArrayList<>();
    for (PlanNutricionalGalpon.Etapa e : actual.etapas()) {
      if ("OMITIDA".equals(e.estado()) || "COMPLETADA".equals(e.estado())) {
        etapas.add(e);
        continue;
      }
      if (edad > e.diaFin()) {
        if ("FUTURA".equals(e.estado()))
          proyecciones.activarHistorica(
              new PlanNutricionalGalpon(
                  actual.id(),
                  actual.galponId(),
                  actual.loteId(),
                  actual.codigoLote(),
                  actual.plantillaId(),
                  actual.tipoAve(),
                  actual.fechaIngreso(),
                  actual.edadIngresoDias(),
                  poblacionViva,
                  actual.fechaSalidaProyectada(),
                  actual.estado(),
                  actual.etapas()),
              e);
        proyecciones.cerrar(e.id());
        e =
            new PlanNutricionalGalpon.Etapa(
                e.id(),
                e.etapaCrianza(),
                "COMPLETADA",
                e.diaInicio(),
                e.diaFin(),
                e.racionKgAveDia(),
                e.tipoAlimento(),
                e.alimentoId(),
                e.pesoNominalPorBultoKg());
      } else if (edad >= e.diaInicio() && "FUTURA".equals(e.estado())) {
        proyecciones.activar(
            new PlanNutricionalGalpon(
                actual.id(),
                actual.galponId(),
                actual.loteId(),
                actual.codigoLote(),
                actual.plantillaId(),
                actual.tipoAve(),
                actual.fechaIngreso(),
                actual.edadIngresoDias(),
                poblacionViva,
                actual.fechaSalidaProyectada(),
                actual.estado(),
                actual.etapas()),
            e);
        e =
            new PlanNutricionalGalpon.Etapa(
                e.id(),
                e.etapaCrianza(),
                "ACTIVA",
                e.diaInicio(),
                e.diaFin(),
                e.racionKgAveDia(),
                e.tipoAlimento(),
                e.alimentoId(),
                e.pesoNominalPorBultoKg());
      }
      etapas.add(e);
    }
    String estado = edad > etapas.get(etapas.size() - 1).diaFin() ? "FINALIZADO" : "ACTIVO";
    PlanNutricionalGalpon actualizado =
        new PlanNutricionalGalpon(
            actual.id(),
            actual.galponId(),
            actual.loteId(),
            actual.codigoLote(),
            actual.plantillaId(),
            actual.tipoAve(),
            actual.fechaIngreso(),
            actual.edadIngresoDias(),
            poblacionViva,
            actual.fechaSalidaProyectada(),
            estado,
            etapas);
    planes.actualizar(actualizado);
    return actualizado;
  }

  @Scheduled(cron = "0 5 0 * * *", zone = "America/Bogota")
  @Transactional
  public void sincronizarEtapasDiarias() {
    List<UUID> lotes = planes.lotesActivos();
    for (UUID loteId : lotes) {
      PlanNutricionalGalpon plan = planes.buscarPorLote(loteId).orElseThrow();
      sincronizarEtapas(loteId, plan.poblacionViva(), LocalDate.now(clock));
    }
  }

  public List<AlimentoCompatibleQueryPort.Alimento> compatibles(EtapaCrianza tipo) {
    return alimentos.compatibles(tipo);
  }

  public List<DetalleEtapa> detalle(UUID galponId) {
    PlanNutricionalGalpon plan = plan(galponId);
    return plan.etapas().stream()
        .map(
            e -> {
              BigDecimal demanda =
                  e.racionKgAveDia().multiply(BigDecimal.valueOf(plan.poblacionViva()));
              BigDecimal bultos =
                  demanda.divide(e.pesoNominalPorBultoKg(), 4, RoundingMode.HALF_UP);
              BigDecimal stock =
                  e.alimentoId() == null
                      ? BigDecimal.ZERO
                      : alimentos
                          .buscar(e.alimentoId())
                          .map(AlimentoCompatibleQueryPort.Alimento::stockDisponibleKg)
                          .orElse(BigDecimal.ZERO);
              BigDecimal autonomia =
                  demanda.signum() == 0 ? null : stock.divide(demanda, 2, RoundingMode.HALF_UP);
              return new DetalleEtapa(e, demanda, bultos, autonomia);
            })
        .toList();
  }

  private PlanNutricionalPlantilla plantilla(UUID id) {
    return plantillas
        .buscar(id)
        .orElseThrow(() -> new NoSuchElementException("Plantilla no encontrada"));
  }

  private PlanNutricionalGalpon plan(UUID galponId) {
    return planes
        .buscarPorGalpon(galponId)
        .orElseThrow(() -> new NoSuchElementException("Plan no encontrado"));
  }

  private int edadActual(PlanNutricionalGalpon p) {
    return p.edadIngresoDias()
        + (int) java.time.temporal.ChronoUnit.DAYS.between(p.fechaIngreso(), LocalDate.now(clock));
  }

  private PlanNutricionalGalpon construir(Alojamiento a, PlanNutricionalPlantilla plantilla) {
    List<PlanNutricionalGalpon.Etapa> etapas = new ArrayList<>();
    if (plantilla != null)
      for (PlanNutricionalPlantilla.Etapa e : plantilla.etapas()) {
        String estado =
            e.diaFin() < a.edadIngresoDias()
                ? "OMITIDA"
                : e.diaInicio() <= a.edadIngresoDias() ? "ACTIVA" : "FUTURA";
        etapas.add(
            new PlanNutricionalGalpon.Etapa(
                UUID.randomUUID(),
                e.etapaCrianza(),
                estado,
                e.diaInicio(),
                e.diaFin(),
                e.racionKgAveDia(),
                e.tipoAlimento(),
                e.alimentoId(),
                e.pesoNominalPorBultoKg()));
      }
    LocalDate salida =
        a.fechaIngreso()
            .plusDays(
                plantilla == null ? 0 : plantilla.etapas().get(2).diaFin() - a.edadIngresoDias());
    return new PlanNutricionalGalpon(
        UUID.randomUUID(),
        a.galponId(),
        a.loteId(),
        a.codigoLote(),
        plantilla == null ? null : plantilla.id(),
        a.tipoAve(),
        a.fechaIngreso(),
        a.edadIngresoDias(),
        a.poblacionViva(),
        salida,
        plantilla == null ? "PENDIENTE_ASIGNACION" : "ACTIVO",
        etapas);
  }

  private PlanNutricionalGalpon nuevoPlan(
      PlanNutricionalGalpon actual, UUID plantillaId, List<PlanNutricionalGalpon.Etapa> etapas) {
    LocalDate salida =
        actual
            .fechaIngreso()
            .plusDays(etapas.get(etapas.size() - 1).diaFin() - actual.edadIngresoDias());
    return new PlanNutricionalGalpon(
        actual.id(),
        actual.galponId(),
        actual.loteId(),
        actual.codigoLote(),
        plantillaId,
        actual.tipoAve(),
        actual.fechaIngreso(),
        actual.edadIngresoDias(),
        actual.poblacionViva(),
        salida,
        actual.estado(),
        etapas);
  }

  public record Alojamiento(
      UUID galponId,
      UUID loteId,
      String codigoLote,
      String tipoAve,
      LocalDate fechaIngreso,
      int edadIngresoDias,
      int poblacionViva) {
    public Alojamiento(
        UUID galponId,
        UUID loteId,
        String tipoAve,
        LocalDate fechaIngreso,
        int edadIngresoDias,
        int poblacionViva) {
      this(galponId, loteId, null, tipoAve, fechaIngreso, edadIngresoDias, poblacionViva);
    }
  }

  public record DetalleEtapa(
      PlanNutricionalGalpon.Etapa etapa,
      BigDecimal demandaDiariaKg,
      BigDecimal demandaDiariaBultos,
      BigDecimal diasAutonomia) {}

  public record PlanNutricionalPendienteEvent(
      UUID galponId, UUID loteId, String tipoAve, String prioridad) {}

  public record FechaSalidaProyectadaActualizadaEvent(
      UUID galponId, UUID loteId, LocalDate fechaAnterior, LocalDate fechaNueva) {}
}
