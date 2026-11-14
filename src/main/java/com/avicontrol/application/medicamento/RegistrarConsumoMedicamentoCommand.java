package com.avicontrol.application.medicamento;

import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarConsumoMedicamentoCommand(
    UUID diagnosticoId,
    BigDecimal cantidad,
    UnidadConsumoMedicamento unidad,
    LocalDate fechaAplicacion,
    String actor,
    UUID idempotenciaClave) {}
