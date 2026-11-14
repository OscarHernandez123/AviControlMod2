package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!local & !test")
public class GalponQueryNoDisponibleAdapter implements GalponQueryPort {
  @Override
  public Optional<String> estadoVigente(UUID galponId) {
    throw new InformacionGalponNoDisponibleException();
  }

  @Override
  public Optional<GalponDatos> consultar(UUID galponId) {
    throw new InformacionGalponNoDisponibleException();
  }

  @Override
  public List<GalponDatos> todos() {
    throw new InformacionGalponNoDisponibleException();
  }

  @Override
  public PaginaGalpones listar(int pagina, int tamano, String campoOrden, boolean ascendente) {
    throw new InformacionGalponNoDisponibleException();
  }
}
