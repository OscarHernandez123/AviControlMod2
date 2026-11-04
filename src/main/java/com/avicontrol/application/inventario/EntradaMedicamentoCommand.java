package com.avicontrol.application.inventario;

import java.math.BigDecimal;
import java.util.UUID;

public record EntradaMedicamentoCommand(
    UUID recepcionId, BigDecimal cantidadUnidadBase, String unidadBase) {}
