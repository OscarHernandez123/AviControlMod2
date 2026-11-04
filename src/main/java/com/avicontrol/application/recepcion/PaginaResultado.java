package com.avicontrol.application.recepcion;

import java.util.List;

public record PaginaResultado<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {}
