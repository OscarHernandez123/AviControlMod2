package com.avicontrol.application.alerta;

import com.avicontrol.application.alerta.result.AlertaSanitariaResult;
import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.port.out.alerta.AlertaSanitariaQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ConsultarAlertasSanitariasUseCase {
  private final AlertaSanitariaQueryPort alertas;
  private final GalponQueryPort galpones;
  private final LoteQueryPort lotes;
  private final Clock clock;

  public ConsultarAlertasSanitariasUseCase(
      AlertaSanitariaQueryPort alertas,
      GalponQueryPort galpones,
      LoteQueryPort lotes,
      Clock clock) {
    this.alertas = alertas;
    this.galpones = galpones;
    this.lotes = lotes;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PaginaAlertas listar(String estado, int page, int size) {
    if (page < 0 || size < 1 || size > 100 || !"PENDIENTE_ATENCION_VETERINARIA".equals(estado))
      throw new IllegalArgumentException("Filtro o paginación inválidos");
    long total = alertas.contar(estado);
    var elementos = alertas.listarIds(estado, page, size).stream().map(this::detalle).toList();
    return new PaginaAlertas(elementos, total, page, size);
  }

  public record PaginaAlertas(
      List<AlertaSanitariaResult> elementos, long totalElementos, int page, int size) {}

  @Transactional(readOnly = true)
  public AlertaSanitariaResult detalle(UUID id) {
    var alerta =
        alertas.buscar(id).orElseThrow(() -> new NoSuchElementException("Alerta no encontrada"));
    var galpon =
        galpones
            .consultar(alerta.galponId())
            .orElseThrow(InformacionGalponNoDisponibleException::new);
    var lote =
        lotes
            .consultarActual(alerta.galponId())
            .orElseThrow(InformacionGalponNoDisponibleException::new);
    if (!lote.id().equals(alerta.loteId()) || lote.fechaIngreso() == null)
      throw new InformacionGalponNoDisponibleException();
    long edad = ChronoUnit.DAYS.between(lote.fechaIngreso(), LocalDate.now(clock)) + 1;
    return new AlertaSanitariaResult(
        alerta.id(),
        alerta.prioridad(),
        alerta.estado(),
        alerta.galponId(),
        alerta.loteId(),
        galpon.nombre(),
        galpon.aforoMaximo(),
        galpon.estado(),
        lote.poblacionActual(),
        edad,
        alerta.cantidadMuertes(),
        alerta.porcentajeMortalidad(),
        alerta.causaProbable(),
        alerta.observaciones(),
        alerta.responsable());
  }
}
