package com.avicontrol.application.catalogo;

import com.avicontrol.domain.model.catalogo.EstadoProductoCatalogo;
import com.avicontrol.domain.model.catalogo.TipoAlimento;
import java.math.BigDecimal;

public record AlimentoCommand(
    String nombreComercial,
    String marca,
    TipoAlimento tipo,
    BigDecimal pesoNominalBultoKg,
    Integer duracionRecomendadaEtapaDias,
    EstadoProductoCatalogo estado) {}
