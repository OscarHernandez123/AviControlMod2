package com.avicontrol.domain.port.out.recepcion;

/** Publishes reception events only after the surrounding transaction commits. */
public interface RecepcionEventPublisherPort {
  void publish(Object event);
}
