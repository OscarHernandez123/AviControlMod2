package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionMedicamentoResumenResponse(
    UUID id,
    UUID medicamentoId,
    String nombreMedicamento,
    String codigoLote,
    String presentacion,
    EstadoRecepcion estado,
    LocalDate fechaIngreso,
    int cantidadPresentaciones,
    BigDecimal contenidoPorPresentacion,
    BigDecimal precioNetoPorPresentacion) {}
