package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import java.util.UUID;

public record GalponListadoResponse(UUID id, String nombre, EstadoGalpon estado) {}
