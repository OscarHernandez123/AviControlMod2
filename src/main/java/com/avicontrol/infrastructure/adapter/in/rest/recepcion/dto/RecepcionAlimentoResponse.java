package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionAlimentoResponse(
    UUID id,
    UUID alimentoId,
    String nombreAlimento,
    EtapaCrianza tipoAlimento,
    EstadoRecepcion estado,
    String codigoLote,
    int cantidadBultos,
    BigDecimal pesoPorBultoKg,
    BigDecimal precioNetoPorBulto,
    BigDecimal impuestoPorcentaje,
    BigDecimal cantidadTotalKg,
    BigDecimal precioNetoKg,
    BigDecimal precioNetoTotal,
    BigDecimal valorImpuesto,
    BigDecimal precioTotal,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento) {}
