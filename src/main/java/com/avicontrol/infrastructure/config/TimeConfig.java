package com.avicontrol.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        // Zona horaria de negocio exigida por General.md
        return Clock.system(ZoneId.of("America/Bogota"));
    }
}
