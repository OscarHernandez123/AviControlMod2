package com.avicontrol.application.support;

import java.time.Instant;

public record BasePingResult(String status, String actorId, Instant occurredAt) {}
