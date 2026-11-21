package com.avicontrol.infrastructure.adapter.in.rest.alimentacion.mapper;

import com.avicontrol.application.alimentacion.result.AlimentoDiarioGalponResult;
import com.avicontrol.application.alimentacion.result.ResumenAlimentoDiarioResult;
import com.avicontrol.domain.model.alimentacion.SuministroDiarioAlimento;
import com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto.AlimentoDiarioGalponResponse;
import com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto.ResumenAlimentoDiarioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto.SuministroDiarioResponse;
import org.springframework.stereotype.Component;

@Component
public class AlimentacionDiariaRestMapper {
  public AlimentoDiarioGalponResponse response(AlimentoDiarioGalponResult r) {
    var q = r.requerimiento();
    return new AlimentoDiarioGalponResponse(
        r.galponId(),
        r.nombreGalpon(),
        r.aforoMaximo(),
        r.estadoGalpon(),
        r.loteId(),
        r.loteCodigo(),
        r.poblacionViva(),
        r.edadDias(),
        r.fecha(),
        r.estado(),
        r.etapaCrianza(),
        r.tipoAlimento(),
        r.alimentoId(),
        r.nombreAlimento(),
        q.racionGramosAve(),
        r.pesoNominalBultoKg(),
        q.requerimientoKg(),
        q.requerimientoBultos(),
        q.suministradoKg(),
        q.saldoKg(),
        q.saldoBultos(),
        q.disponibilidadKg(),
        q.disponibilidadBultos(),
        r.alertas());
  }

  public ResumenAlimentoDiarioResponse response(ResumenAlimentoDiarioResult r) {
    return new ResumenAlimentoDiarioResponse(
        r.fecha(),
        r.requerimientoTotalKg(),
        r.requerimientoTotalBultos(),
        r.estadoBodega(),
        r.galpones().stream()
            .map(
                f ->
                    new ResumenAlimentoDiarioResponse.Fila(
                        f.detalle().galponId(),
                        f.detalle().nombreGalpon(),
                        f.detalle().loteCodigo(),
                        f.detalle().tipoAlimento(),
                        f.detalle().nombreAlimento(),
                        f.detalle().requerimiento().requerimientoKg(),
                        f.detalle().requerimiento().requerimientoBultos(),
                        f.estadoBodega()))
            .toList());
  }

  public SuministroDiarioResponse response(SuministroDiarioAlimento s) {
    return new SuministroDiarioResponse(
        s.id(),
        s.galponId(),
        s.loteId(),
        s.alimentoId(),
        s.tipoAlimento(),
        s.cantidadKg(),
        s.fechaAplicacion(),
        s.fechaRegistro(),
        s.trabajador(),
        s.idempotenciaClave(),
        s.detalles().stream()
            .map(
                d ->
                    new SuministroDiarioResponse.Detalle(
                        d.recepcionId(), d.cantidadKg(), d.precioNetoKg(), "COP"))
            .toList());
  }
}
