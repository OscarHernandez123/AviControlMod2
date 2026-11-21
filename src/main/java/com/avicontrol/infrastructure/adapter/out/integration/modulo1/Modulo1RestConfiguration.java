package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("modulo1-rest & !local & !test")
public class Modulo1RestConfiguration {
  @Bean
  Modulo1RestClient modulo1RestClient(
      @Value("${avicontrol.modulo1.rest.base-url}") URI baseUrl,
      @Value("${avicontrol.modulo1.rest.connect-timeout:2s}") Duration connectTimeout,
      @Value("${avicontrol.modulo1.rest.read-timeout:5s}") Duration readTimeout) {
    return new Modulo1RestClient(baseUrl, connectTimeout, readTimeout);
  }
}
