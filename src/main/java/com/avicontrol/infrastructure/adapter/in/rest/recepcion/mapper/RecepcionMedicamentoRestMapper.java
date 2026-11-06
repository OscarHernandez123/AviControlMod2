package com.avicontrol.infrastructure.adapter.in.rest.recepcion.mapper;

import com.avicontrol.application.recepcion.EditarRecepcionMedicamentoCommand;
import com.avicontrol.application.recepcion.HistorialRecepcionesMedicamento;
import com.avicontrol.application.recepcion.RegistrarRecepcionMedicamentoCommand;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.EditarRecepcionMedicamentoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.HistorialMedicamentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RecepcionMedicamentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RecepcionMedicamentoResumenResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RegistrarRecepcionMedicamentoRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RecepcionMedicamentoRestMapper {
  public RegistrarRecepcionMedicamentoCommand command(
      RegistrarRecepcionMedicamentoRequest r, String actor) {
    return new RegistrarRecepcionMedicamentoCommand(
        r.medicamentoId(),
        r.codigoLote(),
        r.presentacion(),
        r.fechaIngreso(),
        r.fechaVencimiento(),
        r.cantidadPresentaciones(),
        r.contenidoPorPresentacion(),
        r.unidadContenido(),
        r.precioNetoPorPresentacion(),
        r.impuestoPorcentaje(),
        actor);
  }

  public RecepcionMedicamentoResponse response(RecepcionMedicamento r) {
    return new RecepcionMedicamentoResponse(
        r.id(),
        r.medicamentoId(),
        r.nombreMedicamento(),
        r.estado(),
        r.codigoLote(),
        r.presentacion(),
        r.cantidadPresentaciones(),
        r.contenidoPorPresentacion(),
        r.unidadContenido(),
        r.precioNetoPorPresentacion(),
        r.impuestoPorcentaje(),
        r.cantidadUnidadBase(),
        r.unidadBase(),
        r.precioNetoUnidadBase(),
        r.precioNetoTotal(),
        r.valorImpuesto(),
        r.precioTotal(),
        r.fechaIngreso(),
        r.fechaVencimiento());
  }

  public EditarRecepcionMedicamentoCommand command(
      UUID id, EditarRecepcionMedicamentoRequest r, String actor) {
    return new EditarRecepcionMedicamentoCommand(
        id,
        r.medicamentoId(),
        r.fechaVencimiento(),
        r.cantidadPresentaciones(),
        r.contenidoPorPresentacion(),
        r.unidadContenido(),
        r.precioNetoPorPresentacion(),
        r.impuestoPorcentaje(),
        actor);
  }

  public EditarRecepcionMedicamentoRequest formulario(RecepcionMedicamento r) {
    return new EditarRecepcionMedicamentoRequest(
        r.medicamentoId(),
        r.fechaVencimiento(),
        r.cantidadPresentaciones(),
        r.contenidoPorPresentacion(),
        r.unidadContenido(),
        r.precioNetoPorPresentacion(),
        r.impuestoPorcentaje());
  }

  public HistorialMedicamentoResponse historial(HistorialRecepcionesMedicamento resultado) {
    var pagina = resultado.pagina();
    return new HistorialMedicamentoResponse(
        pagina.contenido().stream()
            .map(
                r ->
                    new RecepcionMedicamentoResumenResponse(
                        r.id(),
                        r.medicamentoId(),
                        r.nombreMedicamento(),
                        r.codigoLote(),
                        r.presentacion(),
                        r.estado(),
                        r.fechaIngreso(),
                        r.cantidadPresentaciones(),
                        r.contenidoPorPresentacion(),
                        r.precioNetoPorPresentacion()))
            .toList(),
        pagina.total(),
        pagina.pagina(),
        pagina.tamano(),
        resultado.recepcionesMes());
  }
}
