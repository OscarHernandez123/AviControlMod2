package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import static org.junit.jupiter.api.Assertions.*;

import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class Modulo1RestConfigurationTest {
  @Test
  void perfilRemotoRegistraUnAdaptadorPorPuertoYConfiguraTimeouts() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment().setActiveProfiles("modulo1-rest");
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "test",
                  Map.of(
                      "avicontrol.modulo1.rest.base-url", "http://localhost:8080",
                      "avicontrol.modulo1.rest.connect-timeout", "2s",
                      "avicontrol.modulo1.rest.read-timeout", "5s")));
      context
          .getBeanFactory()
          .setConversionService(ApplicationConversionService.getSharedInstance());
      context.register(Modulo1RestConfiguration.class);
      registrar(context);
      context.refresh();
      assertInstanceOf(GalponRestQueryAdapter.class, context.getBean(GalponQueryPort.class));
      assertInstanceOf(LoteRestQueryAdapter.class, context.getBean(LoteQueryPort.class));
      assertInstanceOf(
          GalponAislamientoRestQueryAdapter.class,
          context.getBean(GalponLoteAislamientoQueryPort.class));
      assertInstanceOf(
          GalponMedicacionRestQueryAdapter.class, context.getBean(GalponLoteQueryPort.class));
    }
  }

  @Test
  void sinPerfilRemotoConservaLosAdaptadoresDeIndisponibilidad() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment().setActiveProfiles("produccion");
      registrar(context);
      context.refresh();
      assertInstanceOf(
          GalponQueryNoDisponibleAdapter.class, context.getBean(GalponQueryPort.class));
      assertInstanceOf(LoteQueryNoDisponibleAdapter.class, context.getBean(LoteQueryPort.class));
      assertInstanceOf(
          GalponNoDisponibleAdapter.class, context.getBean(GalponLoteAislamientoQueryPort.class));
      assertInstanceOf(
          GalponLoteNoDisponibleAdapter.class, context.getBean(GalponLoteQueryPort.class));
    }
  }

  @Test
  void localYTestConservanLosCuatroAdaptadoresJdbc() {
    for (String perfil : new String[] {"local", "test"}) {
      try (var context = new AnnotationConfigApplicationContext()) {
        context.getEnvironment().setActiveProfiles(perfil);
        context.registerBean(
            org.springframework.jdbc.core.JdbcTemplate.class,
            () -> org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class));
        registrar(context);
        context.register(
            GalponQueryAdapter.class,
            LoteQueryAdapter.class,
            GalponLocalAdapter.class,
            GalponLoteQueryAdapter.class);
        context.refresh();
        assertInstanceOf(GalponQueryAdapter.class, context.getBean(GalponQueryPort.class));
        assertInstanceOf(LoteQueryAdapter.class, context.getBean(LoteQueryPort.class));
        assertInstanceOf(
            GalponLocalAdapter.class, context.getBean(GalponLoteAislamientoQueryPort.class));
        assertInstanceOf(GalponLoteQueryAdapter.class, context.getBean(GalponLoteQueryPort.class));
      }
    }
  }

  private void registrar(AnnotationConfigApplicationContext context) {
    context.register(
        GalponRestQueryAdapter.class,
        LoteRestQueryAdapter.class,
        GalponAislamientoRestQueryAdapter.class,
        GalponMedicacionRestQueryAdapter.class,
        GalponQueryNoDisponibleAdapter.class,
        LoteQueryNoDisponibleAdapter.class,
        GalponNoDisponibleAdapter.class,
        GalponLoteNoDisponibleAdapter.class);
  }
}
