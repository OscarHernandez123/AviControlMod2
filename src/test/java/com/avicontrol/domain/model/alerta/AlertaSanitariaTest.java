package com.avicontrol.domain.model.alerta;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.domain.model.mortalidad.NivelSeveridadMortalidad;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertaSanitariaTest {
  @Test
  void construyeAlertaConEstadoPendienteYPrioridadDeSeveridad() {
    UUID reporteId = UUID.randomUUID();
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    Instant ahora = Instant.parse("2026-11-20T15:00:00Z");
    RegistroMortalidadAnormal reporte =
        new RegistroMortalidadAnormal(
            reporteId,
            null,
            galponId,
            loteId,
            ahora.minusSeconds(60),
            ahora,
            25,
            1000,
            new BigDecimal("2.50"),
            NivelSeveridadMortalidad.ALTA,
            "Sospecha respiratoria",
            null,
            "trabajador");

    AlertaSanitaria alerta = AlertaSanitaria.de(reporte, ahora);

    assertThat(alerta.id()).isNotNull();
    assertThat(alerta.reporteId()).isEqualTo(reporteId);
    assertThat(alerta.galponId()).isEqualTo(galponId);
    assertThat(alerta.loteId()).isEqualTo(loteId);
    assertThat(alerta.prioridad()).isEqualTo("ALTA");
    assertThat(alerta.estado()).isEqualTo("PENDIENTE_ATENCION_VETERINARIA");
    assertThat(alerta.fechaEmision()).isEqualTo(ahora);
  }
}
