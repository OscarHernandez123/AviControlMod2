package com.avicontrol.application.sanitario.medicacion;

import com.avicontrol.application.sanitario.medicacion.result.MedicacionDiariaGalponResult;
import com.avicontrol.application.sanitario.medicacion.result.ResumenMedicacionDiariaResult;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ConsultarResumenMedicacionDiariaUseCase {
  private final ConsultarMedicacionDiariaGalponUseCase detalle;
  private final Clock clock;

  public ConsultarResumenMedicacionDiariaUseCase(
      ConsultarMedicacionDiariaGalponUseCase detalle, Clock clock) {
    this.detalle = detalle;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ResumenMedicacionDiariaResult consultar(List<UUID> galponIds, LocalDate fechaSolicitada) {
    LocalDate fecha = fechaSolicitada == null ? LocalDate.now(clock) : fechaSolicitada;
    if (galponIds == null || galponIds.stream().anyMatch(id -> id == null))
      throw new IllegalArgumentException("galponIds inválidos");
    List<MedicacionDiariaGalponResult> galpones =
        new LinkedHashSet<>(galponIds).stream().map(id -> detalle.consultar(id, fecha)).toList();
    int conTratamiento = (int) galpones.stream().filter(g -> !g.tratamientos().isEmpty()).count();
    int cantidad = galpones.stream().mapToInt(g -> g.tratamientos().size()).sum();
    String resumen =
        cantidad == 0
            ? "Sin tratamientos hoy"
            : galpones.stream()
                .filter(g -> !g.tratamientos().isEmpty())
                .findFirst()
                .map(g -> g.tratamientos().getFirst().medicamento() + " en " + g.nombreGalpon())
                .orElse("Sin tratamientos hoy");
    return new ResumenMedicacionDiariaResult(fecha, conTratamiento, cantidad, resumen, galpones);
  }
}
