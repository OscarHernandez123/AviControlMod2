package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named = "AVICONTROL_RUN_POSTGRES_TESTS", matches = "true")
class AislamientoPostgresTest extends AislamientoControllerTest {
  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> System.getenv("AVICONTROL_TEST_DB_URL"));
    registry.add("spring.datasource.username", () -> "avicontrol");
    registry.add("spring.datasource.password", () -> "avicontrol");
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }
}
