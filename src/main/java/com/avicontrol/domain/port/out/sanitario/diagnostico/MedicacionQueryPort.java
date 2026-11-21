package com.avicontrol.domain.port.out.sanitario.diagnostico;

import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import java.util.Optional;
import java.util.UUID;

public interface MedicacionQueryPort {
  Optional<Medicacion> buscarMedicacion(UUID id);
}
