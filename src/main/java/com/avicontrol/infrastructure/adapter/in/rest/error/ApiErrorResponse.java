package com.avicontrol.infrastructure.adapter.in.rest.error;

import java.net.URI;
import java.util.List;

public record ApiErrorResponse(
    URI type,
    String title,
    int status,
    String detail,
    String instance,
    String code,
    String correlationId,
    List<String> fieldErrors) {}
