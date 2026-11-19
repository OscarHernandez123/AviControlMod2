package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("modulo1-rest & !local & !test")
public class GalponAislamientoRestQueryAdapter implements GalponLoteAislamientoQueryPort {
  private final Modulo1RestClient client;

  public GalponAislamientoRestQueryAdapter(Modulo1RestClient client) {
    this.client = client;
  }

  @Override
  public Optional<GalponLoteAislamiento> consultar(UUID id) {
    try {
      return client
          .detalle(id)
          .map(
              g -> {
                var l = g.loteActivo();
                return new GalponLoteAislamiento(
                    g.id(),
                    l == null ? null : l.id(),
                    g.estado(),
                    l == null ? 0 : l.poblacionActual(),
                    l == null ? null : l.fechaIngreso());
              });
    } catch (InformacionGalponNoDisponibleException e) {
      throw new Modulo1NoDisponibleException();
    }
  }
}
