package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import java.math.BigDecimal;
import java.util.List;

public record HistorialAlimentoResponse(
    List<RecepcionAlimentoResumenResponse> contenido,
    long total,
    int pagina,
    int tamano,
    long recepcionesMes,
    BigDecimal pesoMesKg) {}
