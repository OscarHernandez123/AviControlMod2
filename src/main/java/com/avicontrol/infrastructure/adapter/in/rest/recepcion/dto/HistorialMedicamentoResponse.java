package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import java.util.List;

public record HistorialMedicamentoResponse(
    List<RecepcionMedicamentoResumenResponse> contenido,
    long total,
    int pagina,
    int tamano,
    long recepcionesMes) {}
