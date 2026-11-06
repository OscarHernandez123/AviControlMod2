package com.avicontrol.infrastructure.adapter.out.persistence.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.PlanNutricionalPlantilla;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalPlantillaRepositoryPort;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PlanNutricionalPlantillaPersistenceAdapter
    implements PlanNutricionalPlantillaRepositoryPort {
  private final JdbcTemplate jdbc;

  public PlanNutricionalPlantillaPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public void guardar(PlanNutricionalPlantilla p) {
    if (p.esPredeterminado())
      jdbc.update(
          "UPDATE plantillas_nutricionales SET es_predeterminado = FALSE WHERE tipo_ave = ? AND id <> ?",
          p.tipoAve(),
          p.id());
    if (buscar(p.id()).isPresent()) {
      jdbc.update(
          "UPDATE plantillas_nutricionales SET nombre=?, tipo_ave=?, es_predeterminado=?, activo=? WHERE id=?",
          p.nombre(),
          p.tipoAve(),
          p.esPredeterminado(),
          p.activo(),
          p.id());
      jdbc.update("DELETE FROM etapas_plantilla_nutricional WHERE plantilla_id=?", p.id());
    } else {
      jdbc.update(
          "INSERT INTO plantillas_nutricionales (id,nombre,tipo_ave,es_predeterminado,activo,fecha_creacion) VALUES (?,?,?,?,?,?)",
          p.id(),
          p.nombre(),
          p.tipoAve(),
          p.esPredeterminado(),
          p.activo(),
          Timestamp.from(p.fechaCreacion()));
    }
    for (PlanNutricionalPlantilla.Etapa e : p.etapas())
      jdbc.update(
          "INSERT INTO etapas_plantilla_nutricional (id,plantilla_id,etapa_crianza,dia_inicio,dia_fin,racion_kg_ave_dia,tipo_alimento,peso_nominal_bulto_kg,alimento_id) VALUES (?,?,?,?,?,?,?,?,?)",
          UUID.randomUUID(),
          p.id(),
          e.etapaCrianza().name(),
          e.diaInicio(),
          e.diaFin(),
          e.racionKgAveDia(),
          e.tipoAlimento().name(),
          e.pesoNominalPorBultoKg(),
          e.alimentoId());
  }

  @Override
  public Optional<PlanNutricionalPlantilla> buscar(UUID id) {
    return jdbc.query(
        "SELECT * FROM plantillas_nutricionales WHERE id=?",
        rs -> rs.next() ? Optional.of(map(rs.getObject("id", UUID.class))) : Optional.empty(),
        id);
  }

  @Override
  public Optional<PlanNutricionalPlantilla> predeterminada(String tipoAve) {
    return jdbc.query(
        "SELECT id FROM plantillas_nutricionales WHERE tipo_ave=? AND es_predeterminado=TRUE AND activo=TRUE",
        rs -> rs.next() ? buscar(rs.getObject(1, UUID.class)) : Optional.empty(),
        tipoAve);
  }

  @Override
  public List<PlanNutricionalPlantilla> listar() {
    return jdbc.query(
        "SELECT id FROM plantillas_nutricionales ORDER BY nombre",
        (rs, row) -> map(rs.getObject(1, UUID.class)));
  }

  private PlanNutricionalPlantilla map(UUID id) {
    List<PlanNutricionalPlantilla.Etapa> etapas =
        jdbc.query(
            "SELECT * FROM etapas_plantilla_nutricional WHERE plantilla_id=? ORDER BY dia_inicio",
            (rs, row) ->
                new PlanNutricionalPlantilla.Etapa(
                    EtapaCrianza.valueOf(rs.getString("etapa_crianza")),
                    rs.getInt("dia_inicio"),
                    rs.getInt("dia_fin"),
                    rs.getBigDecimal("racion_kg_ave_dia"),
                    EtapaCrianza.valueOf(rs.getString("tipo_alimento")),
                    rs.getBigDecimal("peso_nominal_bulto_kg"),
                    rs.getObject("alimento_id", UUID.class)),
            id);
    return jdbc.queryForObject(
        "SELECT * FROM plantillas_nutricionales WHERE id=?",
        (rs, row) ->
            new PlanNutricionalPlantilla(
                id,
                rs.getString("nombre"),
                rs.getString("tipo_ave"),
                rs.getBoolean("es_predeterminado"),
                rs.getBoolean("activo"),
                rs.getTimestamp("fecha_creacion").toInstant(),
                etapas),
        id);
  }
}
