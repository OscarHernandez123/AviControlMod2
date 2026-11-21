package com.avicontrol.infrastructure.config;

import static com.avicontrol.infrastructure.adapter.in.rest.modulo3.ConfirmacionResultadoModulo3Controller.RUTA;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

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
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/integracion/modulo3/lotes/*/*",
                        "/api/lotes/*/alimento-requerido",
                        "/api/ordenes-sacrificio/*/resultados")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, RUTA)
                    .hasRole("MODULO3")
                    .anyRequest()
                    .access(
                        new WebExpressionAuthorizationManager(
                            "isAuthenticated() and !hasRole('MODULO3')")))
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
          String nutricionistaPassword,
      @Value("${avicontrol.security.modulo3.enabled:false}") boolean modulo3Enabled,
      @Value("${avicontrol.security.modulo3.username:modulo3_consulta}") String modulo3Username,
      @Value("${avicontrol.security.modulo3.password:}") String modulo3Password) {
    var usuarios =
        new InMemoryUserDetailsManager(
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
    if (modulo3Enabled) {
      if (modulo3Password == null || modulo3Password.isBlank())
        throw new IllegalArgumentException("Definir contraseña de integración de Módulo 3");
      usuarios.createUser(
          User.withUsername(modulo3Username)
              .password(passwordEncoder.encode(modulo3Password))
              .roles("MODULO3")
              .build());
    }
    return usuarios;
  }
}
