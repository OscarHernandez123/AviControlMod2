package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import java.util.List;

public record ListadoGalponesResponse(
    List<GalponListadoResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    String mensaje) {}
