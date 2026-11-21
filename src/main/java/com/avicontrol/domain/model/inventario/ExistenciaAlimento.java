package com.avicontrol.domain.model.inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExistenciaAlimento(
    UUID alimentoId,
    String alimento,
    String etapaCrianza,
    int recepcionesActivas,
    LocalDate proximoVencimiento,
    BigDecimal stockActualKg,
    BigDecimal demandaKg,
    String estadoCobertura) {}
