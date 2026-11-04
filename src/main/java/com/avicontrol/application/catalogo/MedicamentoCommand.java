package com.avicontrol.application.catalogo;

import com.avicontrol.domain.model.catalogo.EstadoProductoCatalogo;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;

public record MedicamentoCommand(
    String nombreComercial,
    String presentacionCompatible,
    UnidadMedidaMedicamento unidadBase,
    EstadoProductoCatalogo estado) {}
