package com.avicontrol.domain.port.out.alerta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaSanitariaQueryPort {
  long contar(String estado);

  List<UUID> listarIds(String estado, int page, int size);

  Optional<Datos> buscar(UUID id);

  record Datos(
      UUID id,
      String prioridad,
      String estado,
      UUID galponId,
      UUID loteId,
      int cantidadMuertes,
      BigDecimal porcentajeMortalidad,
      String causaProbable,
      String observaciones,
      String responsable) {}
}
