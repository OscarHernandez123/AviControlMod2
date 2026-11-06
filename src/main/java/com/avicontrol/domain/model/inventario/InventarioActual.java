package com.avicontrol.domain.model.inventario;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record InventarioActual(
    List<ExistenciaAlimento> alimentos,
    List<ExistenciaMedicamento> medicamentos,
    Map<String, BigDecimal> tarjetasAlimentoKg) {}
