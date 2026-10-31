package com.avicontrol.application.galpon.result;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import java.util.List;
import java.util.UUID;

public record GalponDetalleResult(
    UUID id,
    String nombre,
    int aforoMaximo,
    EstadoGalpon estado,
    LoteActualResult loteActual,
    List<IncidenciaResult> incidencias) {}
