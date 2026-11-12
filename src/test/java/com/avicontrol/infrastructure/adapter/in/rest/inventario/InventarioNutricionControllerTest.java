package com.avicontrol.infrastructure.adapter.in.rest.inventario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InventarioNutricionControllerTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "true".equals(System.getenv("AVICONTROL_RUN_POSTGRES_TESTS"))
                ? System.getenv("AVICONTROL_TEST_DB_URL")
                : "jdbc:h2:mem:inventario_nutricion_controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    if ("true".equals(System.getenv("AVICONTROL_RUN_POSTGRES_TESTS"))) {
      registry.add("spring.datasource.username", () -> "avicontrol");
      registry.add("spring.datasource.password", () -> "avicontrol");
      registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
  }

  @Autowired MockMvc mvc;

  @Test
  void openApiIncluyeEndpointsDeInventarioNutricionYProyecciones() throws Exception {
    mvc.perform(
            get("/v3/api-docs")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/inventario'].get").exists())
        .andExpect(jsonPath("$.paths['/api/inventario/resumen'].get").exists())
        .andExpect(jsonPath("$.paths['/api/nutricion/plantillas'].post").exists())
        .andExpect(jsonPath("$.paths['/api/lotes/{loteId}/alimento-requerido'].get").exists());
  }

  @Test
  void inventarioRequiereAdministrador() throws Exception {
    mvc.perform(
            get("/api/inventario")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.alimentos").isArray())
        .andExpect(jsonPath("$.medicamentos").isArray())
        .andExpect(jsonPath("$.tarjetasMedicamento.TOTAL").exists());
    mvc.perform(
            get("/api/inventario")
                .with(SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void plantillasRequierenRolAutorizado() throws Exception {
    mvc.perform(
            get("/api/nutricion/plantillas")
                .with(
                    SecurityMockMvcRequestPostProcessors.user("nutricionista")
                        .roles("NUTRICIONISTA")))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/nutricion/plantillas")
                .with(SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void registraAlimentoComercialYPlantillaConEtapas() throws Exception {
    mvc.perform(
            post("/api/nutricion/alimentos")
                .with(
                    SecurityMockMvcRequestPostProcessors.user("nutricionista")
                        .roles("NUTRICIONISTA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "id": "dba231d2-f0e0-471f-bc3d-bef219911234",
                  "nombre": "Alimento Inicio Broiler",
                  "tipoAlimento": "INICIO",
                  "pesoNominalPorBultoKg": 40,
                  "proteinaPorcentaje": 20,
                  "energiaKcalKg": 3100,
                  "duracionRecomendadaEtapaDias": 14
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estadoStock").value("SIN_STOCK"));
    mvc.perform(
            post("/api/nutricion/plantillas")
                .with(
                    SecurityMockMvcRequestPostProcessors.user("nutricionista")
                        .roles("NUTRICIONISTA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "nombre": "Broiler local",
                  "tipoAve": "BROILER",
                  "esPredeterminado": true,
                  "etapas": [
                    {"etapaCrianza":"PRE_INICIO","diaInicio":1,"diaFin":7,"racionKgAveDia":0.05,"tipoAlimento":"PRE_INICIO","pesoNominalPorBultoKg":40},
                    {"etapaCrianza":"INICIO","diaInicio":8,"diaFin":21,"racionKgAveDia":0.10,"tipoAlimento":"INICIO","pesoNominalPorBultoKg":40,"alimentoId":"dba231d2-f0e0-471f-bc3d-bef219911234"},
                    {"etapaCrianza":"ENGORDE","diaInicio":22,"diaFin":45,"racionKgAveDia":0.12,"tipoAlimento":"ENGORDE","pesoNominalPorBultoKg":40}
                  ]
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.etapas.length()").value(3));
  }
}
