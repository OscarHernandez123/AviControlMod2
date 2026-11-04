package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import java.time.LocalDate;

public record FiltroRecepcionMedicamento(
    String busqueda,
    String presentacion,
    EstadoRecepcion estado,
    LocalDate desde,
    LocalDate hasta,
    int page,
    int size) {}
