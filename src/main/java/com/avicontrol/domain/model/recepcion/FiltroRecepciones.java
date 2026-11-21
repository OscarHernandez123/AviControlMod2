package com.avicontrol.domain.model.recepcion;

import java.time.LocalDate;

public record FiltroRecepciones(
    String busqueda,
    EstadoRecepcion estado,
    LocalDate desde,
    LocalDate hasta,
    String presentacion,
    int pagina,
    int tamano) {}
