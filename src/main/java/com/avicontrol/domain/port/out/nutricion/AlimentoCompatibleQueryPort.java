package com.avicontrol.domain.port.out.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlimentoCompatibleQueryPort {
  List<Alimento> compatibles(EtapaCrianza tipo);

  Optional<Alimento> buscar(UUID id);

  record Alimento(
      UUID id,
      String nombre,
      EtapaCrianza tipoAlimento,
      BigDecimal pesoNominalPorBultoKg,
      BigDecimal proteinaPorcentaje,
      BigDecimal energiaKcalKg,
      Integer duracionRecomendadaEtapaDias,
      BigDecimal stockDisponibleKg,
      BigDecimal stockDisponibleBultos,
      String estadoStock) {}
}
