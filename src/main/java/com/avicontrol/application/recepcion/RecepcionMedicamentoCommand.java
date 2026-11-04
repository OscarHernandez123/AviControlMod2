package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionMedicamentoCommand(
    UUID medicamentoId,
    String codigoLote,
    String presentacion,
    BigDecimal cantidad,
    BigDecimal contenidoPorPresentacion,
    UnidadMedidaMedicamento unidadMedida,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento,
    BigDecimal precioNetoPorPresentacion,
    BigDecimal porcentajeImpuesto) {}
