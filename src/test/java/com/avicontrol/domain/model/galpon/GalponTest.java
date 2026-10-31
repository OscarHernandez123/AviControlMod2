package com.avicontrol.domain.model.galpon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GalponTest {

  @Test
  void requiresANameAndPositiveCapacity() {
    assertThatThrownBy(() -> new Galpon(UUID.randomUUID(), "", 8000, EstadoGalpon.PRODUCTIVO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Galpon(UUID.randomUUID(), "Galpón", 0, EstadoGalpon.PRODUCTIVO))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
