package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.recepcion.PaginaRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;

public record HistorialRecepcionesMedicamento(
    PaginaRecepciones<RecepcionMedicamento> pagina, long recepcionesMes) {}
