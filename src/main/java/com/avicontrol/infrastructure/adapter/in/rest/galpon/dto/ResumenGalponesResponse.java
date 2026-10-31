package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import java.util.Map;

public record ResumenGalponesResponse(
    long totalRegistrado,
    long totalValido,
    long registrosInconsistentes,
    Map<EstadoGalpon, Long> porEstado) {}
