package com.avicontrol.application.galpon.result;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import java.util.Map;

public record ResumenGalponesResult(
    long totalRegistrado,
    long totalValido,
    long registrosInconsistentes,
    Map<EstadoGalpon, Long> porEstado) {}
