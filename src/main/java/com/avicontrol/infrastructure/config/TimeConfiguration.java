package com.avicontrol.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

  public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

  @Bean
  Clock businessClock() {
    return Clock.system(BUSINESS_ZONE);
  }
}
