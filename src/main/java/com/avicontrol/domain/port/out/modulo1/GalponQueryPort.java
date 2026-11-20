package com.avicontrol.domain.port.out.modulo1;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GalponQueryPort {
  Optional<String> estadoVigente(UUID galponId);

  Optional<GalponDatos> consultar(UUID galponId);

  List<GalponDatos> todos();

  PaginaGalpones listar(int pagina, int tamano, String campoOrden, boolean ascendente);

  record GalponDatos(UUID id, String nombre, int aforoMaximo, String estado, LoteDatos loteActual) {
    public GalponDatos(UUID id, String nombre, int aforoMaximo, String estado) {
      this(id, nombre, aforoMaximo, estado, null);
    }
  }

  record LoteDatos(UUID id, String nombre) {}

  record PaginaGalpones(List<GalponDatos> elementos, long totalElementos) {}
}
