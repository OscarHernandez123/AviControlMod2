package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sanitario.aislamiento.CrearSolicitudAislamientoUseCase;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SolicitudAislamientoBeanConfiguration {
  @Bean
  CrearSolicitudAislamientoUseCase crearSolicitudAislamiento(
      SolicitudAislamientoRepositoryPort solicitudes,
      GalponLoteAislamientoQueryPort galpones,
      Clock clock) {
    return new CrearSolicitudAislamientoUseCase(solicitudes, galpones, clock);
  }

  @Bean
  @Profile({"local", "test"})
  ApplicationRunner galponLocalFixture(
      JdbcTemplate jdbc,
      @Value("${avicontrol.modulo1.local.galpon-id:550e8400-e29b-41d4-a716-446655440101}")
          UUID galponId,
      @Value("${avicontrol.modulo1.local.lote-id:550e8400-e29b-41d4-a716-446655440102}")
          UUID loteId) {
    return args -> {
      Integer count =
          jdbc.queryForObject(
              "SELECT COUNT(*) FROM modulo1_galpon_local WHERE id = ?", Integer.class, galponId);
      if (count != null && count == 0) {
        jdbc.update(
            "INSERT INTO modulo1_galpon_local (id, lote_id, estado, poblacion_viva, fecha_ingreso) VALUES (?, ?, ?, ?, ?)",
            galponId,
            loteId,
            "PRODUCTIVA",
            8000,
            LocalDate.of(2026, 11, 1));
      }
    };
  }
}
