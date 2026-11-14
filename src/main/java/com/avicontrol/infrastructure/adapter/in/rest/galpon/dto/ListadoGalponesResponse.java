package com.avicontrol.infrastructure.adapter.in.rest.galpon.dto;

import java.util.List;
import java.util.UUID;

public record ListadoGalponesResponse(
    List<Elemento> elementos,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas,
    String mensaje) {
  public record Elemento(UUID id, String nombre, String estado) {}
}
