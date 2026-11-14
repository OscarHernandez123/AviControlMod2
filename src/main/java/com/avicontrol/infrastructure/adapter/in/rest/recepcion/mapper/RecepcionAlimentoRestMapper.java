package com.avicontrol.infrastructure.adapter.in.rest.recepcion.mapper;

import com.avicontrol.application.recepcion.EditarRecepcionAlimentoCommand;
import com.avicontrol.application.recepcion.HistorialRecepcionesAlimento;
import com.avicontrol.application.recepcion.RegistrarRecepcionAlimentoCommand;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.EditarRecepcionAlimentoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.HistorialAlimentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RecepcionAlimentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RecepcionAlimentoResumenResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RegistrarRecepcionAlimentoRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RecepcionAlimentoRestMapper {
  public RegistrarRecepcionAlimentoCommand command(
      RegistrarRecepcionAlimentoRequest r, String actor) {
    return new RegistrarRecepcionAlimentoCommand(
        r.alimentoId(),
        r.tipoAlimento(),
        r.codigoLote(),
        r.fechaIngreso(),
        r.fechaVencimiento(),
        r.cantidadBultos(),
        r.pesoPorBultoKg(),
        r.precioNetoPorBulto(),
        r.impuestoPorcentaje(),
        actor);
  }

  public RecepcionAlimentoResponse response(RecepcionAlimento r) {
    return new RecepcionAlimentoResponse(
        r.id(),
        r.alimentoId(),
        r.nombreAlimento(),
        r.tipoAlimento(),
        r.estado(),
        r.codigoLote(),
        r.cantidadBultos(),
        r.pesoPorBultoKg(),
        r.precioNetoPorBulto(),
        r.impuestoPorcentaje(),
        r.cantidadTotalKg(),
        r.precioNetoKg(),
        r.precioNetoTotal(),
        r.valorImpuesto(),
        r.precioTotal(),
        r.fechaIngreso(),
        r.fechaVencimiento());
  }

  public EditarRecepcionAlimentoCommand command(
      UUID id, EditarRecepcionAlimentoRequest r, String actor) {
    return new EditarRecepcionAlimentoCommand(
        id,
        r.alimentoId(),
        r.fechaVencimiento(),
        r.cantidadBultos(),
        r.pesoPorBultoKg(),
        r.precioNetoPorBulto(),
        r.impuestoPorcentaje(),
        actor);
  }

  public EditarRecepcionAlimentoRequest formulario(RecepcionAlimento r) {
    return new EditarRecepcionAlimentoRequest(
        r.alimentoId(),
        r.fechaVencimiento(),
        r.cantidadBultos(),
        r.pesoPorBultoKg(),
        r.precioNetoPorBulto(),
        r.impuestoPorcentaje());
  }

  public HistorialAlimentoResponse historial(HistorialRecepcionesAlimento resultado) {
    var pagina = resultado.pagina();
    return new HistorialAlimentoResponse(
        pagina.contenido().stream()
            .map(
                r ->
                    new RecepcionAlimentoResumenResponse(
                        r.id(),
                        r.alimentoId(),
                        r.nombreAlimento(),
                        r.tipoAlimento(),
                        r.codigoLote(),
                        r.estado(),
                        r.fechaIngreso(),
                        r.cantidadBultos(),
                        r.pesoPorBultoKg(),
                        r.precioNetoPorBulto()))
            .toList(),
        pagina.total(),
        pagina.pagina(),
        pagina.tamano(),
        resultado.recepcionesMes(),
        resultado.pesoMesKg());
  }
}
