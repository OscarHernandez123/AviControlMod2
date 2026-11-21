package com.avicontrol.infrastructure.adapter.in.rest.recepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecepcionFlujosControllerTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
    return SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR");
  }

  private String registrar(String ruta, String json) throws Exception {
    String location =
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json).with(admin()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    return location.substring(location.lastIndexOf('/') + 1);
  }

  @Test
  void publicaLasRutasEnOpenApi() throws Exception {
    String json =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(json.contains("/api/recepciones/alimentos"));
    assertTrue(json.contains("/api/recepciones/medicamentos"));
    assertTrue(json.contains("put"));
    assertTrue(json.contains("get"));
  }

  @Test
  void registraConsultaYEditaAlimentoConAuditoria() throws Exception {
    String ruta = "/api/recepciones/alimentos";
    String json =
        """
        {"alimentoId":"550e8400-e29b-41d4-a716-446655440000","tipoAlimento":"ENGORDE",
        "codigoLote":"AL-FLUJO","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-03-05",
        "cantidadBultos":2,"pesoPorBultoKg":40,"precioNetoPorBulto":100,"impuestoPorcentaje":19}
        """;
    String id = registrar(ruta, json);
    mvc.perform(
            get(ruta)
                .param("busqueda", "AL-FLUJO")
                .param("estado", "CONFIRMADA")
                .param("desde", "2026-11-01")
                .param("hasta", "2026-11-30")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.contenido[0].tipoAlimento").value("ENGORDE"))
        .andExpect(jsonPath("$.recepcionesMes").isNumber());
    mvc.perform(get(ruta + "/" + id).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreAlimento").value("Alimento de prueba"))
        .andExpect(jsonPath("$.tipoAlimento").value("ENGORDE"));
    mvc.perform(
            get(ruta)
                .param("busqueda", id)
                .param("desde", "2026-11-01")
                .param("hasta", "2026-11-30")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
    mvc.perform(get(ruta + "/" + id + "/edicion").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cantidadBultos").value(2))
        .andExpect(jsonPath("$.codigoLote").doesNotExist())
        .andExpect(jsonPath("$.tipoAlimento").doesNotExist());
    String cambio =
        """
        {"alimentoId":"550e8400-e29b-41d4-a716-446655440000",
        "fechaVencimiento":"2027-04-05","cantidadBultos":3,"pesoPorBultoKg":20,
        "precioNetoPorBulto":200,"impuestoPorcentaje":19}
        """;
    mvc.perform(
            put(ruta + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cambio)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cantidadTotalKg").value(60))
        .andExpect(jsonPath("$.tipoAlimento").value("ENGORDE"))
        .andExpect(jsonPath("$.codigoLote").value("AL-FLUJO"));
    UUID uuid = UUID.fromString(id);
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM movimientos_inventario WHERE recepcion_id = ? AND cantidad = 60",
            Integer.class,
            uuid));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM auditoria_recepciones WHERE recepcion_id = ? AND actor = 'admin'",
            Integer.class,
            uuid));
    String anteriores =
        jdbc.queryForObject(
            "SELECT valores_anteriores FROM auditoria_recepciones WHERE recepcion_id = ?",
            String.class,
            uuid);
    String nuevos =
        jdbc.queryForObject(
            "SELECT valores_nuevos FROM auditoria_recepciones WHERE recepcion_id = ?",
            String.class,
            uuid);
    assertTrue(anteriores.contains("\"cantidadBultos\":2"));
    assertTrue(nuevos.contains("\"cantidadBultos\":3"));
    mvc.perform(get(ruta).param("busqueda", "SIN-RESULTADOS").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.contenido").isEmpty());
    mvc.perform(
            get(ruta + "/" + id)
                .with(SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void bloqueaEdicionDeAlimentoConMovimientoPosterior() throws Exception {
    String ruta = "/api/recepciones/alimentos";
    String json =
        """
        {"alimentoId":"550e8400-e29b-41d4-a716-446655440000","tipoAlimento":"INICIO",
        "codigoLote":"AL-BLOQUEO","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-03-05",
        "cantidadBultos":1,"pesoPorBultoKg":40,"precioNetoPorBulto":100,"impuestoPorcentaje":0}
        """;
    String id = registrar(ruta, json);
    UUID uuid = UUID.fromString(id);
    jdbc.update(
        "INSERT INTO movimientos_inventario (id, recepcion_id, producto_id, tipo_producto, tipo_movimiento, cantidad, unidad, actor, fecha_registro, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        uuid,
        UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
        "ALIMENTO",
        "SALIDA",
        new BigDecimal("1"),
        "KG",
        "admin",
        Timestamp.from(Instant.now()),
        "CONFIRMADO");
    mvc.perform(get(ruta + "/" + id + "/edicion").with(admin())).andExpect(status().isConflict());
    mvc.perform(
            put(ruta + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"alimentoId":"550e8400-e29b-41d4-a716-446655440000","fechaVencimiento":"2027-04-05",
            "cantidadBultos":2,"pesoPorBultoKg":40,"precioNetoPorBulto":100,"impuestoPorcentaje":0}
            """)
                .with(admin()))
        .andExpect(status().isConflict());
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM auditoria_recepciones WHERE recepcion_id = ?",
            Integer.class,
            uuid));
  }

  @Test
  void registraConsultaYEditaMedicamento() throws Exception {
    String ruta = "/api/recepciones/medicamentos";
    String json =
        """
        {"medicamentoId":"550e8400-e29b-41d4-a716-446655440000","codigoLote":"MED-FLUJO",
        "presentacion":"Frasco","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-05-05",
        "cantidadPresentaciones":2,"contenidoPorPresentacion":500,"unidadContenido":"MILILITRO",
        "precioNetoPorPresentacion":100,"impuestoPorcentaje":0}
        """;
    String id = registrar(ruta, json);
    mvc.perform(
            get(ruta)
                .param("busqueda", "MED-FLUJO")
                .param("presentacion", "Frasco")
                .param("desde", "2026-11-01")
                .param("hasta", "2026-11-30")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.contenido[0].nombreMedicamento").value("Medicamento de prueba"));
    mvc.perform(get(ruta + "/" + id).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.presentacion").value("Frasco"));
    mvc.perform(
            get(ruta)
                .param("busqueda", "Medicamento de prueba")
                .param("presentacion", "Frasco")
                .param("desde", "2026-11-01")
                .param("hasta", "2026-11-30")
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contenido[0].nombreMedicamento").value("Medicamento de prueba"));
    mvc.perform(get(ruta).param("busqueda", "SIN-MEDICAMENTOS").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.contenido").isEmpty());
    mvc.perform(get(ruta + "/" + id + "/edicion").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.presentacion").doesNotExist());
    mvc.perform(
            put(ruta + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"medicamentoId":"550e8400-e29b-41d4-a716-446655440000",
            "fechaVencimiento":"2027-06-05","cantidadPresentaciones":3,
            "contenidoPorPresentacion":250,"unidadContenido":"MILILITRO",
            "precioNetoPorPresentacion":200,"impuestoPorcentaje":19}
            """)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cantidadUnidadBase").value(750))
        .andExpect(jsonPath("$.presentacion").value("Frasco"));
    UUID uuid = UUID.fromString(id);
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM movimientos_inventario WHERE recepcion_id = ? AND cantidad = 750",
            Integer.class,
            uuid));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM auditoria_recepciones WHERE recepcion_id = ?",
            Integer.class,
            uuid));
  }

  @Test
  void bloqueaEdicionDeMedicamentoPorUnidadYMovimiento() throws Exception {
    String ruta = "/api/recepciones/medicamentos";
    String id =
        registrar(
            ruta,
            """
        {"medicamentoId":"550e8400-e29b-41d4-a716-446655440000","codigoLote":"MED-BLOQUEO",
        "presentacion":"Frasco","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-05-05",
        "cantidadPresentaciones":1,"contenidoPorPresentacion":500,"unidadContenido":"MILILITRO",
        "precioNetoPorPresentacion":100,"impuestoPorcentaje":0}
        """);
    String cambio =
        """
        {"medicamentoId":"550e8400-e29b-41d4-a716-446655440000",
        "fechaVencimiento":"2027-06-05","cantidadPresentaciones":2,
        "contenidoPorPresentacion":500,"unidadContenido":"MILILITRO",
        "precioNetoPorPresentacion":100,"impuestoPorcentaje":0}
        """;
    mvc.perform(
            put(ruta + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cambio.replace("MILILITRO", "GRAMO"))
                .with(admin()))
        .andExpect(status().isConflict());
    UUID uuid = UUID.fromString(id);
    jdbc.update(
        "INSERT INTO movimientos_inventario (id, recepcion_id, producto_id, tipo_producto, tipo_movimiento, cantidad, unidad, actor, fecha_registro, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        uuid,
        UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
        "MEDICAMENTO",
        "SALIDA",
        new BigDecimal("1"),
        "MILILITRO",
        "admin",
        Timestamp.from(Instant.now()),
        "CONFIRMADO");
    mvc.perform(get(ruta + "/" + id + "/edicion").with(admin())).andExpect(status().isConflict());
    mvc.perform(
            put(ruta + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cambio)
                .with(admin()))
        .andExpect(status().isConflict());
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM auditoria_recepciones WHERE recepcion_id = ?",
            Integer.class,
            uuid));
  }

  @Test
  void conservaEntregasIndependientesConElMismoLote() throws Exception {
    String alimento =
        """
        {"alimentoId":"550e8400-e29b-41d4-a716-446655440000","tipoAlimento":"INICIO",
        "codigoLote":"AL-COMPARTIDO","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-03-05",
        "cantidadBultos":1,"pesoPorBultoKg":40,"precioNetoPorBulto":100,"impuestoPorcentaje":0}
        """;
    assertNotEquals(
        registrar("/api/recepciones/alimentos", alimento),
        registrar("/api/recepciones/alimentos", alimento));
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_alimento WHERE codigo_lote = 'AL-COMPARTIDO'",
            Integer.class));
    String medicamento =
        """
        {"medicamentoId":"550e8400-e29b-41d4-a716-446655440000","codigoLote":"MED-COMPARTIDO",
        "presentacion":"Frasco","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-05-05",
        "cantidadPresentaciones":1,"contenidoPorPresentacion":500,"unidadContenido":"MILILITRO",
        "precioNetoPorPresentacion":100,"impuestoPorcentaje":0}
        """;
    assertNotEquals(
        registrar("/api/recepciones/medicamentos", medicamento),
        registrar("/api/recepciones/medicamentos", medicamento));
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_medicamento WHERE codigo_lote = 'MED-COMPARTIDO'",
            Integer.class));
  }
}
