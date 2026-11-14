package com.avicontrol.domain.model.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlanNutricionalGalpon(
    UUID id,
    UUID galponId,
    UUID loteId,
    String codigoLote,
    UUID plantillaId,
    String tipoAve,
    LocalDate fechaIngreso,
    int edadIngresoDias,
    int poblacionViva,
    LocalDate fechaSalidaProyectada,
    String estado,
    List<Etapa> etapas) {
  public record Etapa(
      UUID id,
      EtapaCrianza etapaCrianza,
      String estado,
      int diaInicio,
      int diaFin,
      BigDecimal racionKgAveDia,
      EtapaCrianza tipoAlimento,
      UUID alimentoId,
      BigDecimal pesoNominalPorBultoKg) {
    public int dias() {
      return diaFin - diaInicio + 1;
    }
  }
}
