package com.avicontrol.infrastructure.adapter.out.persistence.alimentacion;

import com.avicontrol.domain.port.out.alimentacion.ObservabilidadAlimentacionPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ObservabilidadAlimentacionAdapter implements ObservabilidadAlimentacionPort {
  private final MeterRegistry metrics;

  public ObservabilidadAlimentacionAdapter(MeterRegistry metrics) {
    this.metrics = metrics;
  }

  @Override
  public void registrarDeficit() {
    metrics.counter("avicontrol.alimentacion.consulta.deficit").increment();
  }
}
