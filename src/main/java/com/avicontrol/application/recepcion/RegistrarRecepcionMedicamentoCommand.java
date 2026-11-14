package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarRecepcionMedicamentoCommand(
    UUID medicamentoId,
    String codigoLote,
    String presentacion,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento,
    int cantidadPresentaciones,
    BigDecimal contenidoPorPresentacion,
    UnidadMedidaMedicamento unidadContenido,
    BigDecimal precioNetoPorPresentacion,
    BigDecimal impuestoPorcentaje,
    String actor) {}
