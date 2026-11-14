package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.util.UUID;

public interface CatalogoRecepcionPort {
  boolean alimentoExiste(UUID id);

  String nombreAlimento(UUID id);

  String nombreMedicamento(UUID id);

  UnidadMedidaMedicamento unidadBaseMedicamento(UUID id);
}
