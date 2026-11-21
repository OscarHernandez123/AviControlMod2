package com.avicontrol.application.sacrificio.productivo.result;

import java.util.List;
import java.util.UUID;

public record ValidacionAptitudResult(
    UUID galponId,
    UUID loteId,
    boolean apto,
    List<String> motivosNoAptitud,
    String estadoGalponResultante) {}
