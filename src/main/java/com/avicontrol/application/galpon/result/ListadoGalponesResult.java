package com.avicontrol.application.galpon.result;

import java.util.List;

public record ListadoGalponesResult(
    List<GalponListadoResult> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    String mensaje) {}
