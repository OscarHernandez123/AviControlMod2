package com.avicontrol.infrastructure.config;

import com.avicontrol.infrastructure.error.SecurityProblemResponder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Development-only authentication; production authentication remains externally configurable. */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, SecurityProblemResponder problemResponder) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/api/base/ping")
                    .hasRole("ADMINISTRADOR")
                    .requestMatchers("/api/galpones/resumen", "/api/galpones/*/edad-lote")
                    .hasRole("ADMINISTRADOR")
                    .requestMatchers("/api/galpones/**")
                    .hasAnyRole("ADMINISTRADOR", "TRABAJADOR", "USUARIO")
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            problemResponder.write(
                                request,
                                response,
                                org.springframework.http.HttpStatus.UNAUTHORIZED,
                                "AUTENTICACION_REQUERIDA",
                                "Autenticación requerida",
                                "Debe autenticarse para acceder al recurso solicitado"))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            problemResponder.write(
                                request,
                                response,
                                org.springframework.http.HttpStatus.FORBIDDEN,
                                "ACCESO_NO_AUTORIZADO",
                                "Acceso no autorizado",
                                "No tiene permisos para acceder al recurso solicitado")))
        .httpBasic(Customizer.withDefaults())
        .build();
  }

  @Bean
  UserDetailsService userDetailsService(
      @Value("${avicontrol.security.admin-password}") String adminPassword,
      @Value("${avicontrol.security.nutricionista-password}") String nutricionistaPassword,
      @Value("${avicontrol.security.trabajador-password}") String trabajadorPassword,
      @Value("${avicontrol.security.usuario-password}") String usuarioPassword) {
    return new InMemoryUserDetailsManager(
        User.withUsername("administrador")
            .password("{noop}" + adminPassword)
            .authorities("ROLE_ADMINISTRADOR")
            .build(),
        User.withUsername("nutricionista")
            .password("{noop}" + nutricionistaPassword)
            .authorities("ROLE_NUTRICIONISTA")
            .build(),
        User.withUsername("trabajador")
            .password("{noop}" + trabajadorPassword)
            .authorities("ROLE_TRABAJADOR")
            .build(),
        User.withUsername("usuario")
            .password("{noop}" + usuarioPassword)
            .authorities("ROLE_USUARIO")
            .build());
  }
}
