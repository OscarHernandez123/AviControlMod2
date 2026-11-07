package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.mapper;

import com.avicontrol.application.sacrificio.productivo.result.OrdenSacrificioResult;
import com.avicontrol.application.sacrificio.productivo.result.ResumenOrdenesSacrificioResult;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.OrdenSacrificioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ResumenOrdenesSacrificioResponse;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class SacrificioProductivoRestMapper {
  private static final ZoneId ZONA = ZoneId.of("America/Bogota");

  public OrdenSacrificioResponse response(OrdenSacrificioResult result) {
    return new OrdenSacrificioResponse(
        result.id(),
        result.galponId(),
        result.loteId(),
        result.estado(),
        result.fechaHoraProgramada().atZone(ZONA).toOffsetDateTime());
  }

  public ResumenOrdenesSacrificioResponse response(ResumenOrdenesSacrificioResult result) {
    return new ResumenOrdenesSacrificioResponse(result.totalOrdenes(), result.ordenesPendientes());
  }
}
