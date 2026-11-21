package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EditarRecepcionMedicamentoCommand(
    UUID id,
    UUID medicamentoId,
    LocalDate fechaVencimiento,
    int cantidadPresentaciones,
    BigDecimal contenidoPorPresentacion,
    UnidadMedidaMedicamento unidadContenido,
    BigDecimal precioNetoPorPresentacion,
    BigDecimal impuestoPorcentaje,
    String actor) {}
