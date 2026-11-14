package com.avicontrol.application.alimentacion;

import com.avicontrol.application.alimentacion.result.AlimentoDiarioGalponResult;
import com.avicontrol.application.alimentacion.result.ResumenAlimentoDiarioResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

public class ConsultarResumenAlimentoDiarioUseCase {
  private final ConsultarAlimentoGalponDiaUseCase detalle;
  private final Clock clock;

  public ConsultarResumenAlimentoDiarioUseCase(
      ConsultarAlimentoGalponDiaUseCase detalle, Clock clock) {
    this.detalle = detalle;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ResumenAlimentoDiarioResult consultar(List<UUID> galponIds, LocalDate fechaSolicitada) {
    if (galponIds == null || galponIds.stream().anyMatch(id -> id == null))
      throw new IllegalArgumentException("galponIds inválidos");
    LocalDate fecha = fechaSolicitada == null ? LocalDate.now(clock) : fechaSolicitada;
    if (!fecha.equals(LocalDate.now(clock)))
      throw new IllegalArgumentException("La consulta usa la jornada actual");
    List<AlimentoDiarioGalponResult> activos =
        new LinkedHashSet<>(galponIds)
            .stream()
                .map(id -> detalle.consultar(id, fecha))
                .filter(d -> d.requerimiento().requerimientoKg().signum() > 0)
                .toList();
    Map<UUID, BigDecimal> demandaPorAlimento =
        activos.stream()
            .filter(d -> d.alimentoId() != null)
            .collect(
                Collectors.groupingBy(
                    AlimentoDiarioGalponResult::alimentoId,
                    Collectors.reducing(
                        BigDecimal.ZERO,
                        d -> d.requerimiento().requerimientoKg(),
                        BigDecimal::add)));
    List<ResumenAlimentoDiarioResult.Fila> filas =
        activos.stream()
            .map(
                d ->
                    new ResumenAlimentoDiarioResult.Fila(
                        d,
                        d.alimentoId() != null
                                && d.requerimiento()
                                        .disponibilidadKg()
                                        .compareTo(demandaPorAlimento.get(d.alimentoId()))
                                    >= 0
                            ? "SUFICIENTE"
                            : "INSUFICIENTE"))
            .toList();
    BigDecimal kg =
        activos.stream()
            .map(d -> d.requerimiento().requerimientoKg())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal bultos =
        activos.stream()
            .map(d -> d.requerimiento().requerimientoBultos())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    String estado =
        filas.stream().anyMatch(f -> "INSUFICIENTE".equals(f.estadoBodega()))
            ? "BODEGA_INSUFICIENTE"
            : "BODEGA_CENTRAL_OK";
    return new ResumenAlimentoDiarioResult(fecha, kg, bultos, estado, filas);
  }
}
