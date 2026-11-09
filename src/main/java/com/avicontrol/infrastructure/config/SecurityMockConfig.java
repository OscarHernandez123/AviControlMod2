package com.avicontrol.infrastructure.config;

import com.avicontrol.infrastructure.adapter.in.rest.shared.ProblemJsonWriter;
import com.avicontrol.infrastructure.security.MockRoleFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile({"dev", "test"})
@EnableMethodSecurity
public class SecurityMockConfig {

  @Bean
  FilterRegistrationBean<MockRoleFilter> mockRoleFilterRegistration(MockRoleFilter filter) {
    FilterRegistrationBean<MockRoleFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  SecurityFilterChain mockSecurityFilterChain(
      HttpSecurity http, MockRoleFilter mockRoleFilter, ProblemJsonWriter problemJsonWriter)
      throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            problemJsonWriter.write(
                                request,
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "NO_AUTENTICADO",
                                "La solicitud requiere autenticación."))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            problemJsonWriter.write(
                                request,
                                response,
                                HttpStatus.FORBIDDEN,
                                "ACCESO_DENEGADO",
                                "El rol no tiene permiso para esta operación.")))
        .addFilterBefore(mockRoleFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
