package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("modulo1-rest & !local & !test")
public class GalponMedicacionRestQueryAdapter implements GalponLoteQueryPort {
  private final Modulo1RestClient client;

  public GalponMedicacionRestQueryAdapter(Modulo1RestClient client) {
    this.client = client;
  }

  @Override
  public Optional<GalponLote> consultar(UUID id) {
    try {
      return client
          .detalle(id)
          .map(
              g -> {
                var l = g.loteActivo();
                return new GalponLote(
                    g.id(),
                    g.nombre(),
                    Math.toIntExact(g.capacidad()),
                    g.estado(),
                    l == null ? null : l.id(),
                    l == null ? null : l.nombre(),
                    l == null ? 0 : l.poblacionActual(),
                    l == null ? null : l.fechaIngreso());
              });
    } catch (InformacionGalponNoDisponibleException e) {
      throw new Modulo1NoDisponibleException();
    }
  }
}
