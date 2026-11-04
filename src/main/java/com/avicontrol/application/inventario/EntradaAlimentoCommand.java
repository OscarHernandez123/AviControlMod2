package com.avicontrol.application.inventario;

import java.math.BigDecimal;
import java.util.UUID;

public record EntradaAlimentoCommand(UUID recepcionId, BigDecimal cantidadKg) {}
