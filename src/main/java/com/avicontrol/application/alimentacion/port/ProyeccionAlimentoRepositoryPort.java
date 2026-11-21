package com.avicontrol.application.alimentacion.port;

import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase.HistorialAjuste;
import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase.Proyeccion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProyeccionAlimentoRepositoryPort {
  void bloquearEtapa(UUID etapaId);

  boolean existe(UUID etapaId);

  void crear(Proyeccion proyeccion);

  Proyeccion buscarPorEtapa(UUID etapaId);

  Proyeccion buscar(UUID id);

  List<Proyeccion> listarPorLote(UUID loteId);

  boolean actualizarDuracion(UUID id, long version, int diasProrroga, BigDecimal kilogramos);

  void registrarAjuste(UUID proyeccionId, HistorialAjuste ajuste);

  void cerrar(UUID etapaId);

  boolean completarCosto(UUID id, BigDecimal costo, Instant fecha);

  void auditarCosto(UUID id, BigDecimal costo, String justificacion, String actor, Instant fecha);

  List<HistorialAjuste> historial(UUID id);

  String nombreAlimento(UUID alimentoId);

  List<CostoSaldo> saldosElegibles(UUID alimentoId, LocalDate fecha);

  List<DemandaAgrupada> demandaActiva();

  BigDecimal stock(UUID alimentoId, BigDecimal peso, LocalDate fecha);

  BigDecimal demandaVigente(UUID alimentoId, String nombreNormalizado, String tipoAlimento);

  record CostoSaldo(BigDecimal precio, BigDecimal saldo) {}

  record DemandaAgrupada(
      UUID alimentoId, BigDecimal pesoNominalPorBultoKg, BigDecimal demandaConsolidadaKg) {}
}
