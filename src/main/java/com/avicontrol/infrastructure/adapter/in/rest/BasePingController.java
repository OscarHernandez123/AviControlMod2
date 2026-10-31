package com.avicontrol.infrastructure.adapter.in.rest;

import com.avicontrol.application.support.BasePingResult;
import com.avicontrol.application.support.BasePingUseCase;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/base")
public class BasePingController {

  private final BasePingUseCase basePingUseCase;

  public BasePingController(BasePingUseCase basePingUseCase) {
    this.basePingUseCase = basePingUseCase;
  }

  @GetMapping("/ping")
  ResponseEntity<BasePingResponse> ping() {
    BasePingResult result = basePingUseCase.execute();
    return ResponseEntity.ok(
        new BasePingResponse(result.status(), result.actorId(), result.occurredAt()));
  }

  record BasePingResponse(String status, String actorId, Instant occurredAt) {}
}
