package com.avicontrol.domain.port.out.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.util.UUID;

public interface AlimentoComercialRepositoryPort {
  void registrar(
      UUID id,
      String nombre,
      EtapaCrianza tipoAlimento,
      BigDecimal pesoNominalPorBultoKg,
      BigDecimal proteinaPorcentaje,
      BigDecimal energiaKcalKg,
      Integer duracionRecomendadaEtapaDias);
}
