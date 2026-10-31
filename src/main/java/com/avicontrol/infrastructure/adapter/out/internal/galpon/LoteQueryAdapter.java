package com.avicontrol.infrastructure.adapter.out.internal.galpon;

import com.avicontrol.domain.exception.galpon.LoteActualInconsistenteException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.galpon.LoteQueryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LoteQueryAdapter implements LoteQueryPort {

  private final Modulo1GalponesReadApi modulo1GalponesReadApi;

  public LoteQueryAdapter(Modulo1GalponesReadApi modulo1GalponesReadApi) {
    this.modulo1GalponesReadApi = modulo1GalponesReadApi;
  }

  @Override
  public Optional<Lote> buscarActualPorGalponId(UUID galponId) {
    List<Lote> lotes = modulo1GalponesReadApi.findCurrentLotesByGalponId(galponId);
    if (lotes.size() > 1) {
      throw new LoteActualInconsistenteException();
    }
    return lotes.stream().findFirst();
  }
}
