package com.avicontrol.infrastructure.adapter.in.rest.galpon.mapper;

import com.avicontrol.application.galpon.result.EdadLoteResult;
import com.avicontrol.application.galpon.result.GalponDetalleResult;
import com.avicontrol.application.galpon.result.ListadoGalponesResult;
import com.avicontrol.application.galpon.result.ResumenGalponesResult;
import com.avicontrol.domain.model.lote.Edad;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.EdadLoteResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.GalponDetalleResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.ListadoGalponesResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.ResumenGalponesResponse;
import org.springframework.stereotype.Component;

@Component
public class GalponRestMapper {
  public EdadLoteResponse response(EdadLoteResult result) {
    return new EdadLoteResponse(
        result.galponId(), result.loteId(), edad(result.edad()), result.mensaje());
  }

  public GalponDetalleResponse response(GalponDetalleResult result) {
    GalponDetalleResponse.LoteActualResponse lote = null;
    if (result.loteActual() != null) {
      var actual = result.loteActual();
      lote =
          new GalponDetalleResponse.LoteActualResponse(
              actual.id(), actual.poblacionActual(), edad(actual.edad()), actual.mensaje());
    }
    return new GalponDetalleResponse(
        result.id(),
        result.nombre(),
        result.aforoMaximo(),
        result.estado(),
        lote,
        result.mensaje());
  }

  public ListadoGalponesResponse response(ListadoGalponesResult result) {
    return new ListadoGalponesResponse(
        result.elementos().stream()
            .map(g -> new ListadoGalponesResponse.Elemento(g.id(), g.nombre(), g.estado()))
            .toList(),
        result.pagina(),
        result.tamano(),
        result.totalElementos(),
        result.totalPaginas(),
        result.mensaje());
  }

  public ResumenGalponesResponse response(ResumenGalponesResult result) {
    return new ResumenGalponesResponse(
        result.totalRegistrado(),
        result.totalValido(),
        result.registrosInconsistentes(),
        result.porEstado());
  }

  private EdadLoteResponse.EdadResponse edad(Edad edad) {
    return edad == null
        ? null
        : new EdadLoteResponse.EdadResponse(
            edad.diasTotales(), edad.semanasCompletas(), edad.diasRestantes(), edad.texto());
  }
}
