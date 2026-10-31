package com.avicontrol.domain.port.out.galpon;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import java.util.Map;

public record ResumenGalponesFuente(
    long totalRegistrado, long registrosInconsistentes, Map<EstadoGalpon, Long> porEstado) {}
