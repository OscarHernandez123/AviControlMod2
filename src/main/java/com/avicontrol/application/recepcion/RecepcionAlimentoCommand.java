package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.catalogo.TipoAlimento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionAlimentoCommand(
    UUID alimentoId,
    TipoAlimento tipoAlimento,
    String codigoLote,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento,
    BigDecimal cantidadBultos,
    BigDecimal pesoPorBultoKg,
    BigDecimal precioNetoPorBulto,
    BigDecimal porcentajeImpuesto) {}
