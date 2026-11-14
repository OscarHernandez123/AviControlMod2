package com.avicontrol.application.recepcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EditarRecepcionAlimentoCommand(
    UUID id,
    UUID alimentoId,
    LocalDate fechaVencimiento,
    int cantidadBultos,
    BigDecimal pesoPorBultoKg,
    BigDecimal precioNetoPorBulto,
    BigDecimal impuestoPorcentaje,
    String actor) {}
