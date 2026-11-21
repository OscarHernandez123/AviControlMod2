package com.avicontrol.infrastructure.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.mapper.DiagnosticoRestMapper;
import com.avicontrol.infrastructure.adapter.out.integration.modulo1.GalponReintegroLocalAdapter;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class DemoReintegroActivationTest {
  final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(
              DemoReintegroService.class,
              DemoReintegroController.class,
              DiagnosticoRestMapper.class)
          .withBean(
              DiagnosticoGalponRepositoryPort.class,
              () -> mock(DiagnosticoGalponRepositoryPort.class))
          .withBean(
              GalponLoteAislamientoQueryPort.class,
              () -> mock(GalponLoteAislamientoQueryPort.class))
          .withBean(
              GalponReintegroLocalAdapter.class, () -> mock(GalponReintegroLocalAdapter.class))
          .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
          .withBean(Clock.class, Clock::systemUTC);

  @Test
  void desactivadoPorDefectoInclusoEnLocal() {
    runner
        .withPropertyValues("spring.profiles.active=local")
        .run(
            c -> {
              assertTrue(c.getBeansOfType(DemoReintegroController.class).isEmpty());
              assertTrue(c.getBeansOfType(DemoReintegroService.class).isEmpty());
            });
  }

  @Test
  void noDisponibleFueraDeLocalAunqueLaBanderaEsteActiva() {
    runner
        .withPropertyValues("avicontrol.demo.enabled=true")
        .run(
            c -> {
              assertTrue(c.getBeansOfType(DemoReintegroController.class).isEmpty());
              assertTrue(c.getBeansOfType(DemoReintegroService.class).isEmpty());
            });
  }

  @Test
  void disponibleSoloConPerfilLocalYBanderaExplicita() {
    runner
        .withPropertyValues("spring.profiles.active=local", "avicontrol.demo.enabled=true")
        .run(
            c -> {
              assertEquals(1, c.getBeansOfType(DemoReintegroController.class).size());
              assertEquals(1, c.getBeansOfType(DemoReintegroService.class).size());
            });
  }

  @Test
  void excluidoSiSeActivaElPerfilDeIntegracionM1() {
    runner
        .withPropertyValues(
            "spring.profiles.active=local,modulo1-rest", "avicontrol.demo.enabled=true")
        .run(c -> assertTrue(c.getBeansOfType(DemoReintegroController.class).isEmpty()));
  }
}
