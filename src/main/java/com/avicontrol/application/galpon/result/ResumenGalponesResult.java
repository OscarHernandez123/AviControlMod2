package com.avicontrol.application.galpon.result;

import java.util.Map;

public record ResumenGalponesResult(
    int totalRegistrado,
    int totalValido,
    int registrosInconsistentes,
    Map<String, Integer> porEstado) {}
