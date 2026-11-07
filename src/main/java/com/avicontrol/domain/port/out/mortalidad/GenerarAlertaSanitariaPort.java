package com.avicontrol.domain.port.out.mortalidad;

import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import java.util.UUID;

public interface GenerarAlertaSanitariaPort {
  UUID generar(RegistroMortalidadAnormal reporte);
}
