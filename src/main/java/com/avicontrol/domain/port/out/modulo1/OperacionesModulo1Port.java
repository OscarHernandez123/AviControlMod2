package com.avicontrol.domain.port.out.modulo1;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Guarda los datos de negocio junto a la solicitud de entrega, sin afirmar aceptación remota. */
public interface OperacionesModulo1Port {
  record Operacion(UUID id, String tipo, UUID galponId, UUID loteId, Map<String, Object> datos) {}

  java.util.List<Operacion> listar();

  void bloquear(UUID id);

  Optional<Operacion> buscar(UUID id);

  void guardar(Operacion operacion);
}
