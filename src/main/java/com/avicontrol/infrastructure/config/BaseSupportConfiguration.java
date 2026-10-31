package com.avicontrol.infrastructure.config;

import com.avicontrol.application.support.BasePingUseCase;
import com.avicontrol.application.support.CurrentActorProvider;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BaseSupportConfiguration {

  @Bean
  BasePingUseCase basePingUseCase(Clock businessClock, CurrentActorProvider currentActorProvider) {
    return new BasePingUseCase(businessClock, currentActorProvider);
  }
}
