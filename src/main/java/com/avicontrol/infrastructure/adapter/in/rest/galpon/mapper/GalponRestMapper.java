package com.avicontrol.infrastructure.adapter.in.rest.galpon.mapper;

import com.avicontrol.application.galpon.result.EdadLoteResult;
import com.avicontrol.application.galpon.result.GalponDetalleResult;
import com.avicontrol.application.galpon.result.GalponListadoResult;
import com.avicontrol.application.galpon.result.IncidenciaResult;
import com.avicontrol.application.galpon.result.ListadoGalponesResult;
import com.avicontrol.application.galpon.result.LoteActualResult;
import com.avicontrol.application.galpon.result.ResumenGalponesResult;
import com.avicontrol.domain.model.lote.Edad;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.EdadLoteResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.GalponDetalleResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.GalponListadoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.ListadoGalponesResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.ResumenGalponesResponse;
import org.springframework.stereotype.Component;

@Component
public class GalponRestMapper {

  public EdadLoteResponse toResponse(EdadLoteResult result) {
    return new EdadLoteResponse(
        result.galponId(), result.loteId(), toEdadResponse(result.edad()), result.mensaje());
  }

  public GalponDetalleResponse toResponse(GalponDetalleResult result) {
    return new GalponDetalleResponse(
        result.id(),
        result.nombre(),
        result.aforoMaximo(),
        result.estado(),
        toLoteActualResponse(result.loteActual()),
        result.incidencias().stream().map(this::toIncidenciaResponse).toList());
  }

  public ResumenGalponesResponse toResponse(ResumenGalponesResult result) {
    return new ResumenGalponesResponse(
        result.totalRegistrado(),
        result.totalValido(),
        result.registrosInconsistentes(),
        result.porEstado());
  }

  public ListadoGalponesResponse toResponse(ListadoGalponesResult result) {
    return new ListadoGalponesResponse(
        result.content().stream().map(this::toGalponListadoResponse).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.first(),
        result.last(),
        result.mensaje());
  }

  private EdadLoteResponse.EdadResponse toEdadResponse(Edad edad) {
    return edad == null
        ? null
        : new EdadLoteResponse.EdadResponse(
            edad.diasTotales(), edad.semanasCompletas(), edad.diasRestantes(), edad.texto());
  }

  private GalponDetalleResponse.LoteActualResponse toLoteActualResponse(LoteActualResult lote) {
    return lote == null
        ? null
        : new GalponDetalleResponse.LoteActualResponse(
            lote.id(), lote.poblacionActual(), lote.fechaIngreso(), toEdadResponse(lote.edad()));
  }

  private GalponDetalleResponse.IncidenciaResponse toIncidenciaResponse(
      IncidenciaResult incidencia) {
    return new GalponDetalleResponse.IncidenciaResponse(incidencia.codigo(), incidencia.mensaje());
  }

  private GalponListadoResponse toGalponListadoResponse(GalponListadoResult galpon) {
    return new GalponListadoResponse(galpon.id(), galpon.nombre(), galpon.estado());
  }
}
