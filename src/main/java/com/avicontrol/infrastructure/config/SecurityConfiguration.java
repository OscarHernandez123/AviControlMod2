package com.avicontrol.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProblemWriter problemWriter)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(Customizer.withDefaults())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(problemWriter)
                    .accessDeniedHandler(problemWriter))
        .formLogin(formLogin -> formLogin.disable())
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(
      PasswordEncoder passwordEncoder,
      @Value("${avicontrol.security.bootstrap.admin.username}") String adminUsername,
      @Value("${avicontrol.security.bootstrap.admin.password}") String adminPassword,
      @Value("${avicontrol.security.bootstrap.veterinario.username}") String veterinarioUsername,
      @Value("${avicontrol.security.bootstrap.veterinario.password}") String veterinarioPassword,
      @Value("${avicontrol.security.bootstrap.trabajador.username}") String trabajadorUsername,
      @Value("${avicontrol.security.bootstrap.trabajador.password}") String trabajadorPassword,
      @Value("${avicontrol.security.bootstrap.nutricionista.username}")
          String nutricionistaUsername,
      @Value("${avicontrol.security.bootstrap.nutricionista.password}")
          String nutricionistaPassword) {
    return new InMemoryUserDetailsManager(
        User.withUsername(adminUsername)
            .password(passwordEncoder.encode(adminPassword))
            .roles("ADMINISTRADOR")
            .build(),
        User.withUsername(veterinarioUsername)
            .password(passwordEncoder.encode(veterinarioPassword))
            .roles("VETERINARIO")
            .build(),
        User.withUsername(trabajadorUsername)
            .password(passwordEncoder.encode(trabajadorPassword))
            .roles("TRABAJADOR")
            .build(),
        User.withUsername(nutricionistaUsername)
            .password(passwordEncoder.encode(nutricionistaPassword))
            .roles("NUTRICIONISTA")
            .build());
  }
}
