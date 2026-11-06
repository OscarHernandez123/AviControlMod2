package com.avicontrol.application.recepcion;

import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;

public interface AuditoriaRecepcionPort {
  void alimento(RecepcionAlimento antes, RecepcionAlimento despues, String actor);

  void medicamento(RecepcionMedicamento antes, RecepcionMedicamento despues, String actor);
}
