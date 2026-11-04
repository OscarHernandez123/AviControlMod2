package com.avicontrol.domain.port.out.recepcion;

import com.avicontrol.domain.model.recepcion.RegistroAuditoriaRecepcion;

public interface AuditoriaRecepcionRepositoryPort {
  RegistroAuditoriaRecepcion guardar(RegistroAuditoriaRecepcion registro);
}
