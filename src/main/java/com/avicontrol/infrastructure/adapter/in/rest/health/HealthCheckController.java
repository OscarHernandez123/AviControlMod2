package com.avicontrol.infrastructure.adapter.in.rest.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sanitario/health")
public class HealthCheckController {

  private final JdbcTemplate jdbcTemplate;

  public HealthCheckController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public HealthResponse health() {
    Long enfermedades = jdbcTemplate.queryForObject("SELECT count(*) FROM enfermedad", Long.class);
    return new HealthResponse("UP", enfermedades);
  }

  public record HealthResponse(String status, Long enfermedades) {}
}
