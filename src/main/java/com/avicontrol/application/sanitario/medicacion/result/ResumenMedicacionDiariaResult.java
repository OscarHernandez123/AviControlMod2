package com.avicontrol.application.sanitario.medicacion.result;

import java.time.LocalDate;
import java.util.List;

public record ResumenMedicacionDiariaResult(
    LocalDate fecha,
    int galponesConTratamiento,
    int cantidadTratamientos,
    String resumen,
    List<MedicacionDiariaGalponResult> galpones) {
  public ResumenMedicacionDiariaResult {
    galpones = List.copyOf(galpones);
  }
}
