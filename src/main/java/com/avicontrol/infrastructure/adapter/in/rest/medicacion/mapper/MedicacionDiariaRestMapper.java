package com.avicontrol.infrastructure.adapter.in.rest.medicacion.mapper;

import com.avicontrol.application.sanitario.medicacion.result.MedicacionDiariaGalponResult;
import com.avicontrol.application.sanitario.medicacion.result.ResumenMedicacionDiariaResult;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.MedicacionDiariaGalponResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.MedicacionDiariaGalponResponse.TratamientoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.ResumenMedicacionDiariaResponse;
import org.springframework.stereotype.Component;

@Component
public class MedicacionDiariaRestMapper {
  public MedicacionDiariaGalponResponse response(MedicacionDiariaGalponResult result) {
    return new MedicacionDiariaGalponResponse(
        result.galponId(),
        result.nombreGalpon(),
        result.aforoMaximo(),
        result.estadoGalpon(),
        result.loteId(),
        result.loteCodigo(),
        result.poblacionViva(),
        result.edadDias(),
        result.fecha(),
        result.estado(),
        result.mensaje(),
        result.tratamientos().size(),
        result.tratamientos().stream()
            .map(
                t ->
                    new TratamientoResponse(
                        t.diagnosticoId(),
                        t.loteCodigo(),
                        t.progreso().etiqueta(),
                        t.enfermedad(),
                        t.medicamento(),
                        t.principioActivo(),
                        t.dosisHoy(),
                        t.viaAdministracion(),
                        t.aplicaHoy()))
            .toList());
  }

  public ResumenMedicacionDiariaResponse response(ResumenMedicacionDiariaResult result) {
    return new ResumenMedicacionDiariaResponse(
        result.fecha(),
        result.galponesConTratamiento(),
        result.cantidadTratamientos(),
        result.resumen(),
        result.galpones().stream().map(this::response).toList());
  }
}
