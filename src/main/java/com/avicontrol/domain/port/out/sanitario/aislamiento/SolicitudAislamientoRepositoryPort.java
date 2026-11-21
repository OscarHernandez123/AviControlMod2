package com.avicontrol.domain.port.out.sanitario.aislamiento;

import com.avicontrol.domain.model.sanitario.aislamiento.SolicitudAislamiento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudAislamientoRepositoryPort {
  void crear(SolicitudAislamiento solicitud);

  Optional<SolicitudAislamiento> buscar(UUID id);

  boolean existePendiente(UUID galponId);

  List<SolicitudAislamiento> pendientes();

  boolean resolver(SolicitudAislamiento pendiente, SolicitudAislamiento resuelta);
}
