package com.avicontrol.infrastructure.adapter.in.rest.modulo1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
public class CapacidadesModulo1Controller {
  @Value("${avicontrol.modulo1.asincrono:false}")
  private boolean asincrono;

  @GetMapping("/api/integracion/modulo1/capacidades")
  public java.util.Map<String, Boolean> capacidades() {
    return java.util.Map.of("asincrono", asincrono, "sacrificioSanitario", !asincrono);
  }
}
