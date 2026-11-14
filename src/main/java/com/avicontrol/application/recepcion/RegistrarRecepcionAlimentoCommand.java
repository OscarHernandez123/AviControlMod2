package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarRecepcionAlimentoCommand(
    UUID alimentoId,
    EtapaCrianza tipoAlimento,
    String codigoLote,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento,
    int cantidadBultos,
    BigDecimal pesoPorBultoKg,
    BigDecimal precioNetoPorBulto,
    BigDecimal impuestoPorcentaje,
    String actor) {}
