package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.recepcion.PaginaRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import java.math.BigDecimal;

public record HistorialRecepcionesAlimento(
    PaginaRecepciones<RecepcionAlimento> pagina, long recepcionesMes, BigDecimal pesoMesKg) {}
