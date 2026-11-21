package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.application.modulo1.eventos.SolicitarEventoModulo1UseCase;
import com.avicontrol.domain.port.out.modulo1.EventosModulo1Port;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.util.HashMap;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@Profile("modulo1-rest & !local & !test")
public class Modulo1KafkaConfiguration {
  @Bean
  com.avicontrol.application.modulo1.eventos.FlujosKafkaModulo1UseCase flujosKafkaModulo1(
      com.avicontrol.domain.port.out.modulo1.OperacionesModulo1Port operaciones,
      EventosModulo1Port entregas,
      SolicitarEventoModulo1UseCase eventos,
      LoteQueryPort lotes,
      GalponQueryPort galpones,
      com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort
          solicitudes,
      Clock clock) {
    return new com.avicontrol.application.modulo1.eventos.FlujosKafkaModulo1UseCase(
        operaciones, entregas, eventos, lotes, galpones, solicitudes, clock);
  }

  @Bean
  SolicitarEventoModulo1UseCase solicitarEventoModulo1(
      EventosModulo1Port eventos, GalponQueryPort galpones, LoteQueryPort lotes, Clock clock) {
    return new SolicitarEventoModulo1UseCase(eventos, galpones, lotes, clock);
  }

  @Bean
  DefaultKafkaProducerFactory<String, String> modulo1ProducerFactory(KafkaProperties properties) {
    var config = new HashMap<String, Object>(properties.buildProducerProperties());
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
    config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
    config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  KafkaTemplate<String, String> modulo1KafkaTemplate(
      DefaultKafkaProducerFactory<String, String> modulo1ProducerFactory) {
    return new KafkaTemplate<>(modulo1ProducerFactory);
  }
}
