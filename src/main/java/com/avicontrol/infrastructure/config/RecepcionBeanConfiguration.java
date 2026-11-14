package com.avicontrol.infrastructure.config;

import com.avicontrol.application.recepcion.CatalogoRecepcionPort;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class RecepcionBeanConfiguration {

  @Bean
  Clock relojNegocio() {
    return Clock.system(ZoneId.of("America/Bogota"));
  }

  @Bean
  @Profile({"local", "test"})
  CatalogoRecepcionPort catalogoLocal(
      @Value("${avicontrol.catalogo.alimentos:}") String alimentos,
      @Value("${avicontrol.catalogo.medicamentos:}") String medicamentos) {
    Map<UUID, UnidadMedidaMedicamento> bases = new java.util.HashMap<>();
    for (String item : medicamentos.split(",")) {
      if (!item.isBlank()) {
        String[] parts = item.trim().split(":");
        bases.put(UUID.fromString(parts[0]), UnidadMedidaMedicamento.valueOf(parts[1]));
      }
    }
    java.util.Set<UUID> alimentosIds = new java.util.HashSet<>();
    for (String item : alimentos.split(",")) {
      if (!item.isBlank()) alimentosIds.add(UUID.fromString(item.trim()));
    }
    return new CatalogoRecepcionPort() {
      public boolean alimentoExiste(UUID id) {
        return alimentosIds.contains(id);
      }

      public String nombreAlimento(UUID id) {
        return alimentosIds.contains(id) ? "Alimento de prueba" : null;
      }

      public String nombreMedicamento(UUID id) {
        return bases.containsKey(id) ? "Medicamento de prueba" : null;
      }

      public UnidadMedidaMedicamento unidadBaseMedicamento(UUID id) {
        return bases.get(id);
      }
    };
  }
}
