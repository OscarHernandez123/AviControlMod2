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
import org.springframework.jdbc.core.JdbcTemplate;

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
      @Value("${avicontrol.catalogo.medicamentos:}") String medicamentos,
      JdbcTemplate jdbc) {
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
        return alimentosIds.contains(id) || !nombresComerciales(id).isEmpty();
      }

      public String nombreAlimento(UUID id) {
        var nombres = nombresComerciales(id);
        return !nombres.isEmpty()
            ? nombres.getFirst()
            : alimentosIds.contains(id) ? "Alimento de prueba" : null;
      }

      private java.util.List<String> nombresComerciales(UUID id) {
        return jdbc.query(
            "SELECT nombre FROM alimentos_comerciales_nutricion WHERE id=? AND activo=TRUE",
            (rs, row) -> rs.getString("nombre"),
            id);
      }

      public String nombreMedicamento(UUID id) {
        return bases.containsKey(id) ? "Medicamento de prueba" : null;
      }

      public UnidadMedidaMedicamento unidadBaseMedicamento(UUID id) {
        return bases.get(id);
      }

      public java.util.List<MedicamentoDisponible> medicamentosDisponibles() {
        return bases.entrySet().stream()
            .map(
                entry ->
                    new MedicamentoDisponible(
                        entry.getKey(), nombreMedicamento(entry.getKey()), entry.getValue()))
            .sorted(java.util.Comparator.comparing(MedicamentoDisponible::nombre))
            .toList();
      }
    };
  }
}
