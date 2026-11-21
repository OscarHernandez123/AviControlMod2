package com.avicontrol.domain.model.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanNutricionalPlantilla(
    UUID id,
    String nombre,
    String tipoAve,
    boolean esPredeterminado,
    boolean activo,
    Instant fechaCreacion,
    List<Etapa> etapas) {

  public PlanNutricionalPlantilla {
    if (nombre == null || nombre.isBlank() || tipoAve == null || tipoAve.isBlank())
      throw new IllegalArgumentException("Nombre y tipo de ave son obligatorios");
    if (etapas == null || etapas.size() != 3)
      throw new IllegalArgumentException("Se requieren las tres etapas de crianza");
    if (etapas.stream().anyMatch(java.util.Objects::isNull))
      throw new IllegalArgumentException("Las etapas no pueden ser nulas");
    List<Etapa> ordenadas =
        etapas.stream().sorted((a, b) -> Integer.compare(a.diaInicio(), b.diaInicio())).toList();
    int inicio = 1;
    for (Etapa etapa : ordenadas) {
      if (etapa == null || etapa.etapaCrianza() == null || etapa.tipoAlimento() == null)
        throw new IllegalArgumentException("Etapa y tipo de alimento son obligatorios");
      if (etapa.diaInicio() != inicio || etapa.diaFin() < inicio)
        throw new IllegalArgumentException("Los rangos de días deben ser contiguos desde el día 1");
      if (etapa.racionKgAveDia() == null
          || etapa.racionKgAveDia().signum() <= 0
          || etapa.racionKgAveDia().scale() > 4)
        throw new IllegalArgumentException(
            "La ración debe ser positiva y tener hasta cuatro decimales");
      if (etapa.pesoNominalPorBultoKg() == null || etapa.pesoNominalPorBultoKg().signum() <= 0)
        throw new IllegalArgumentException("El peso nominal debe ser positivo");
      if (!etapa.etapaCrianza().name().equals(etapa.tipoAlimento().name()))
        throw new IllegalArgumentException("El tipo de alimento no corresponde a la etapa");
      inicio = etapa.diaFin() + 1;
    }
    if (ordenadas.stream().map(Etapa::etapaCrianza).distinct().count() != 3)
      throw new IllegalArgumentException("Cada etapa de crianza debe aparecer una vez");
    etapas = List.copyOf(ordenadas);
  }

  public record Etapa(
      EtapaCrianza etapaCrianza,
      int diaInicio,
      int diaFin,
      BigDecimal racionKgAveDia,
      EtapaCrianza tipoAlimento,
      BigDecimal pesoNominalPorBultoKg,
      UUID alimentoId) {}
}
