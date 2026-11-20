package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("modulo1-rest & !local & !test")
public class LoteRestQueryAdapter implements LoteQueryPort {
  private final Modulo1RestClient client;

  public LoteRestQueryAdapter(Modulo1RestClient client) {
    this.client = client;
  }

  @Override
  public Optional<Lote> consultarActual(UUID id) {
    return client
        .detalle(id)
        .flatMap(
            g ->
                Optional.ofNullable(g.loteActivo())
                    .map(
                        l ->
                            new Lote(
                                l.id(),
                                g.id(),
                                l.poblacionActual(),
                                l.fechaIngreso(),
                                l.nombre())));
  }
}
