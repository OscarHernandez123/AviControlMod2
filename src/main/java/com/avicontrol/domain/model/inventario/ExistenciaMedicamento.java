package com.avicontrol.domain.model.inventario;

import java.math.BigDecimal;
import java.util.UUID;

public record ExistenciaMedicamento(
    UUID medicamentoId,
    String medicamento,
    String presentacion,
    BigDecimal cantidad,
    BigDecimal contenidoPorPresentacion,
    String unidad,
    BigDecimal stockActual) {}
