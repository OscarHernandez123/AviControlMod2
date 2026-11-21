package com.avicontrol.domain.model.recepcion;

import java.util.List;

public record PaginaRecepciones<T>(List<T> contenido, long total, int pagina, int tamano) {}
