package com.avicontrol.infrastructure.adapter.out.internal.recepcion;

import com.avicontrol.domain.port.out.recepcion.RecepcionEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class RecepcionEventPublisherAdapter implements RecepcionEventPublisherPort {
  private final ApplicationEventPublisher publisher;

  RecepcionEventPublisherAdapter(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  public void publish(Object event) {
    publisher.publishEvent(event);
  }
}
