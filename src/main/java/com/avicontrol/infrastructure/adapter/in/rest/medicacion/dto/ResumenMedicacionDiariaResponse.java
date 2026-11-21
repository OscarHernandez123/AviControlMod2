package com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto;

import java.time.LocalDate;
import java.util.List;

public record ResumenMedicacionDiariaResponse(
    LocalDate fecha,
    int galponesConTratamiento,
    int cantidadTratamientos,
    String resumen,
    List<MedicacionDiariaGalponResponse> galpones) {}
