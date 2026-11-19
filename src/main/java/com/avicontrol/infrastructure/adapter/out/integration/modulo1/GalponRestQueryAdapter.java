package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("modulo1-rest & !local & !test")
public class GalponRestQueryAdapter implements GalponQueryPort {
  private final Modulo1RestClient client;

  public GalponRestQueryAdapter(Modulo1RestClient client) {
    this.client = client;
  }

  @Override
  public Optional<String> estadoVigente(UUID id) {
    return client.detalle(id).map(Modulo1RestClient.Galpon::estado);
  }

  @Override
  public Optional<GalponDatos> consultar(UUID id) {
    return client.detalle(id).map(this::map);
  }

  @Override
  public List<GalponDatos> todos() {
    return client.todos().stream().map(this::map).toList();
  }

  @Override
  public PaginaGalpones listar(int pagina, int tamano, String campo, boolean ascendente) {
    if (pagina < 0 || tamano < 1 || tamano > 100)
      throw new IllegalArgumentException("Paginación inválida");
    Comparator<GalponDatos> orden =
        switch (campo) {
          case "nombre" -> Comparator.comparing(GalponDatos::nombre);
          case "estado" -> Comparator.comparing(GalponDatos::estado);
          case "id" -> Comparator.comparing(g -> g.id().toString());
          default -> throw new IllegalArgumentException("Orden inválido");
        };
    if (!ascendente) orden = orden.reversed();
    var todos = todos();
    var elementos =
        todos.stream()
            .sorted(orden.thenComparing(g -> g.id().toString()))
            .skip((long) pagina * tamano)
            .limit(tamano)
            .toList();
    return new PaginaGalpones(elementos, todos.size());
  }

  private GalponDatos map(Modulo1RestClient.Galpon g) {
    return new GalponDatos(g.id(), g.nombre(), Math.toIntExact(g.capacidad()), g.estado());
  }
}
