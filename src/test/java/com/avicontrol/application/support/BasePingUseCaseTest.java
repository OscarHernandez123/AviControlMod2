package com.avicontrol.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BasePingUseCaseTest {

  @Test
  void returnsTheAuthenticatedActorAndTheTimeProvidedByTheClock() {
    Instant expectedInstant = Instant.parse("2026-10-31T15:30:00Z");
    CurrentActorProvider actorProvider =
        () -> new CurrentActor("administrador", Set.of("ROLE_ADMINISTRADOR"));
    BasePingUseCase useCase =
        new BasePingUseCase(Clock.fixed(expectedInstant, ZoneOffset.UTC), actorProvider);

    BasePingResult result = useCase.execute();

    assertThat(result.status()).isEqualTo("UP");
    assertThat(result.actorId()).isEqualTo("administrador");
    assertThat(result.occurredAt()).isEqualTo(expectedInstant);
  }
}
