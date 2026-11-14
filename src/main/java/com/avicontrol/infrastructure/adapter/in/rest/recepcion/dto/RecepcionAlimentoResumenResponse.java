package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionAlimentoResumenResponse(
    UUID id,
    UUID alimentoId,
    String nombreAlimento,
    EtapaCrianza tipoAlimento,
    String codigoLote,
    EstadoRecepcion estado,
    LocalDate fechaIngreso,
    int cantidadBultos,
    BigDecimal pesoPorBultoKg,
    BigDecimal precioNetoPorBulto) {}
