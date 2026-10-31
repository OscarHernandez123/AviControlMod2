package com.avicontrol.application.support;

import java.time.Clock;

public final class BasePingUseCase {

  private final Clock clock;
  private final CurrentActorProvider currentActorProvider;

  public BasePingUseCase(Clock clock, CurrentActorProvider currentActorProvider) {
    this.clock = clock;
    this.currentActorProvider = currentActorProvider;
  }

  public BasePingResult execute() {
    CurrentActor actor = currentActorProvider.currentActor();
    return new BasePingResult("UP", actor.id(), clock.instant());
  }
}
