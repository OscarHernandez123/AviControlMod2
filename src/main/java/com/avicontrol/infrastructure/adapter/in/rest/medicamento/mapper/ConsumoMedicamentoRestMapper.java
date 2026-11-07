package com.avicontrol.infrastructure.adapter.in.rest.medicamento.mapper;

import com.avicontrol.application.medicamento.RegistrarConsumoMedicamentoCommand;
import com.avicontrol.application.medicamento.result.ConsumoMedicamentoResult;
import com.avicontrol.infrastructure.adapter.in.rest.medicamento.dto.ConsumoMedicamentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicamento.dto.DetalleConsumoMedicamentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicamento.dto.RegistrarConsumoMedicamentoRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConsumoMedicamentoRestMapper {
  public RegistrarConsumoMedicamentoCommand command(
      UUID diagnosticoId, RegistrarConsumoMedicamentoRequest request, String actor, UUID clave) {
    return new RegistrarConsumoMedicamentoCommand(
        diagnosticoId,
        request.cantidad(),
        request.unidad(),
        request.fechaAplicacion(),
        actor,
        clave);
  }

  public ConsumoMedicamentoResponse response(ConsumoMedicamentoResult r) {
    return new ConsumoMedicamentoResponse(
        r.id(),
        r.diagnosticoId(),
        r.galponId(),
        r.loteId(),
        r.medicamentoId(),
        r.cantidadIngresada(),
        r.unidadIngresada(),
        r.cantidadUnidadBase(),
        r.unidadBase(),
        r.fechaAplicacion(),
        r.fechaRegistro(),
        r.actor(),
        r.detallesInventario().stream()
            .map(
                d ->
                    new DetalleConsumoMedicamentoResponse(
                        d.recepcionId(),
                        d.cantidadUnidadBase(),
                        d.precioNetoHistoricoUnidadBase(),
                        d.moneda()))
            .toList());
  }
}
