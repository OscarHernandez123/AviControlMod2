package com.avicontrol.application.alerta.result;

import java.math.BigDecimal;
import java.util.UUID;

public record AlertaSanitariaResult(
    UUID id,
    String prioridad,
    String estado,
    UUID galponId,
    UUID loteId,
    String nombreGalpon,
    Integer aforoMaximo,
    String estadoGalpon,
    Integer poblacionActual,
    Long edadDias,
    int cantidadMuertes,
    BigDecimal porcentajeMortalidad,
    String causaProbable,
    String observaciones,
    String responsable) {}
