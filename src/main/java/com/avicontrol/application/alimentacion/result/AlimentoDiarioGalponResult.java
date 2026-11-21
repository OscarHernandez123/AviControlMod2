package com.avicontrol.application.alimentacion.result;

import com.avicontrol.domain.model.alimentacion.AlertaAlimentacion;
import com.avicontrol.domain.model.alimentacion.RequerimientoDiarioGalpon;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AlimentoDiarioGalponResult(
    UUID galponId,
    String nombreGalpon,
    int aforoMaximo,
    String estadoGalpon,
    UUID loteId,
    String loteCodigo,
    Integer poblacionViva,
    Integer edadDias,
    LocalDate fecha,
    String estado,
    String etapaCrianza,
    String tipoAlimento,
    UUID alimentoId,
    String nombreAlimento,
    java.math.BigDecimal pesoNominalBultoKg,
    RequerimientoDiarioGalpon requerimiento,
    List<AlertaAlimentacion> alertas) {
  public AlimentoDiarioGalponResult {
    alertas = List.copyOf(alertas);
  }
}
