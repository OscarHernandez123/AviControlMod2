package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.mapper;

import com.avicontrol.application.sacrificio.productivo.result.OrdenSacrificioResult;
import com.avicontrol.application.sacrificio.productivo.result.ResumenOrdenesSacrificioResult;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.OrdenSacrificioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ResumenOrdenesSacrificioResponse;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class SacrificioProductivoRestMapper {
  private com.avicontrol.application.modulo1.eventos.FlujosKafkaModulo1UseCase integracion;

  public SacrificioProductivoRestMapper() {}

  @org.springframework.beans.factory.annotation.Autowired
  public SacrificioProductivoRestMapper(
      org.springframework.beans.factory.ObjectProvider<
              com.avicontrol.application.modulo1.eventos.FlujosKafkaModulo1UseCase>
          provider) {
    this.integracion = provider.getIfAvailable();
  }

  private static final ZoneId ZONA = ZoneId.of("America/Bogota");

  public OrdenSacrificioResponse response(OrdenSacrificioResult result) {
    var entrega =
        integracion != null && integracion.existe(result.id())
            ? integracion.consultar(result.id()).integracion()
            : null;
    return new OrdenSacrificioResponse(
        result.id(),
        result.galponId(),
        result.loteId(),
        entrega != null ? "VACIADO_SOLICITADO" : result.estado(),
        result.fechaHoraProgramada().atZone(ZONA).toOffsetDateTime(),
        entrega);
  }

  public ResumenOrdenesSacrificioResponse response(ResumenOrdenesSacrificioResult result) {
    return new ResumenOrdenesSacrificioResponse(result.totalOrdenes(), result.ordenesPendientes());
  }
}
