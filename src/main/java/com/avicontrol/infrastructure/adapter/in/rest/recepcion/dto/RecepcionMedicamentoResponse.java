package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionMedicamentoResponse(
    UUID id,
    UUID medicamentoId,
    String nombreMedicamento,
    EstadoRecepcion estado,
    String codigoLote,
    String presentacion,
    int cantidadPresentaciones,
    BigDecimal contenidoPorPresentacion,
    UnidadMedidaMedicamento unidadContenido,
    BigDecimal precioNetoPorPresentacion,
    BigDecimal impuestoPorcentaje,
    BigDecimal cantidadUnidadBase,
    UnidadMedidaMedicamento unidadBase,
    BigDecimal precioNetoUnidadBase,
    BigDecimal precioNetoTotal,
    BigDecimal valorImpuesto,
    BigDecimal precioTotal,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento) {}
