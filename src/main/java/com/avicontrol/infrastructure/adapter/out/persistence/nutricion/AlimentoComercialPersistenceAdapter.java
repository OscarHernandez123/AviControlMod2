package com.avicontrol.infrastructure.adapter.out.persistence.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.port.out.nutricion.AlimentoComercialRepositoryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlimentoComercialPersistenceAdapter implements AlimentoComercialRepositoryPort {
  private final JdbcTemplate jdbc;

  public AlimentoComercialPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void registrar(
      UUID id,
      String nombre,
      EtapaCrianza tipoAlimento,
      BigDecimal pesoNominalPorBultoKg,
      BigDecimal proteinaPorcentaje,
      BigDecimal energiaKcalKg,
      Integer duracionRecomendadaEtapaDias) {
    jdbc.update(
        "INSERT INTO alimentos_comerciales_nutricion (id,nombre,tipo_alimento,peso_nominal_bulto_kg,proteina_porcentaje,energia_kcal_kg,duracion_recomendada_etapa_dias,activo) VALUES (?,?,?,?,?,?,?,TRUE)",
        id,
        nombre,
        tipoAlimento.name(),
        pesoNominalPorBultoKg,
        proteinaPorcentaje,
        energiaKcalKg,
        duracionRecomendadaEtapaDias);
  }
}
