package com.avicontrol.application.galpon.result;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import java.util.UUID;

public record GalponListadoResult(UUID id, String nombre, EstadoGalpon estado) {}
