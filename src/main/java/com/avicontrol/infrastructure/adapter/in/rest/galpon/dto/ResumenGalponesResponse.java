package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import java.util.Map;

public record ResumenGalponesResponse(
    int totalRegistrado,
    int totalValido,
    int registrosInconsistentes,
    Map<String, Integer> porEstado) {}
