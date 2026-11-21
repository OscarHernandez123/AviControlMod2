package com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto;

import com.avicontrol.domain.model.alimentacion.AlertaAlimentacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AlimentoDiarioGalponResponse(
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
    BigDecimal racionGramosAve,
    BigDecimal pesoNominalBultoKg,
    BigDecimal requerimientoKg,
    BigDecimal requerimientoBultos,
    BigDecimal suministradoKg,
    BigDecimal saldoKg,
    BigDecimal saldoBultos,
    BigDecimal disponibilidadKg,
    BigDecimal disponibilidadBultos,
    List<AlertaAlimentacion> alertas) {}
