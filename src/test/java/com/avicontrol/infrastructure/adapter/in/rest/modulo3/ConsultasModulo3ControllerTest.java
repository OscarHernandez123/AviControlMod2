package com.avicontrol.infrastructure.adapter.in.rest.modulo3;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.infrastructure.adapter.in.event.sacrificio.productivo.Modulo3LiquidacionLoteEventListener;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = AviControlModulo2Application.class,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:consultas_m3;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "avicontrol.security.modulo3.enabled=true",
      "avicontrol.security.modulo3.username=m3_test",
      "avicontrol.security.modulo3.password=solo-pruebas-m3"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsultasModulo3ControllerTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Modulo3LiquidacionLoteEventListener eventos;
  private final UUID lote = UUID.randomUUID();
  private final UUID galpon = UUID.randomUUID();
  private final UUID orden = UUID.randomUUID();
  private final UUID resultado = UUID.randomUUID();

  @BeforeEach
  void resultadoDePrueba() {
    jdbc.update(
        """
        INSERT INTO ordenes_sacrificio_productivo
        (id,galpon_id,lote_id,estado,fecha_hora_programada,fecha_creacion,lote_reservado_id)
        VALUES (?,?,?,'EJECUTADA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?)
        """,
        orden,
        galpon,
        lote,
        lote);
    jdbc.update(
        """
        INSERT INTO resultados_sacrificio_productivo
        (id,orden_id,galpon_id,lote_id,cantidad_final_pollos,peso_total_kg,estado,fecha_registro)
        VALUES (?,?,?,?,100,250.50,'DISPONIBLE',CURRENT_TIMESTAMP)
        """,
        resultado,
        orden,
        galpon,
        lote);
  }

  @Test
  void consultaResultadoPorLoteConBasicAuthYNoPorOrden() throws Exception {
    mvc.perform(get(ruta("resultado-sacrificio")).with(httpBasic("m3_test", "solo-pruebas-m3")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(resultado.toString()))
        .andExpect(jsonPath("$.loteId").value(lote.toString()))
        .andExpect(jsonPath("$.cantidadFinalPollos").value(100));
    mvc.perform(
            get("/api/integracion/modulo3/lotes/{id}/resultado-sacrificio", UUID.randomUUID())
                .with(user("m3").roles("MODULO3")))
        .andExpect(status().isNotFound());
  }

  @Test
  void cuentaM3SoloPermiteConsultasAutorizadas() throws Exception {
    mvc.perform(get(ruta("alimentos"))).andExpect(status().isUnauthorized());
    mvc.perform(get(ruta("alimentos")).with(httpBasic("m3_test", "incorrecta")))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(ruta("alimentos")).with(user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/ordenes-sacrificio/{id}/resultados", orden)
                .with(httpBasic("m3_test", "solo-pruebas-m3")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/galpones").with(httpBasic("m3_test", "solo-pruebas-m3")))
        .andExpect(status().isForbidden());
    mvc.perform(get(ruta("medicamentos")).with(user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk());
  }

  @Test
  void alimentoConservaTramosPreciosHistoricosEImpuestoSinMezclarLotes() throws Exception {
    UUID suministro = UUID.randomUUID();
    suministro(suministro, lote);
    tramoAlimento(suministro, "Inicio A", "2", "10", "19");
    tramoAlimento(suministro, "Inicio B", "3", "20", "5");
    UUID ajeno = UUID.randomUUID();
    suministro(ajeno, UUID.randomUUID());
    tramoAlimento(ajeno, "Otro lote", "99", "30", "0");
    mvc.perform(get(ruta("alimentos")).with(user("m3").roles("MODULO3")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loteId").value(lote.toString()))
        .andExpect(jsonPath("$.partidas.length()").value(2))
        .andExpect(
            jsonPath("$.partidas[?(@.nombreAlimento == 'Inicio A')].cantidadKg")
                .value(org.hamcrest.Matchers.contains(2.0)))
        .andExpect(
            jsonPath("$.partidas[?(@.nombreAlimento == 'Inicio A')].precioNetoKg")
                .value(org.hamcrest.Matchers.contains(10.0)))
        .andExpect(
            jsonPath("$.partidas[?(@.nombreAlimento == 'Inicio A')].impuestoPorcentaje")
                .value(org.hamcrest.Matchers.contains(19.0)))
        .andExpect(
            jsonPath("$.partidas[?(@.nombreAlimento == 'Inicio A')].valorImpuestoCop")
                .value(org.hamcrest.Matchers.contains(3.8)))
        .andExpect(
            jsonPath("$.partidas[?(@.nombreAlimento == 'Inicio A')].valorImpuestoRecepcionCop")
                .value(org.hamcrest.Matchers.contains(19.0)))
        .andExpect(
            jsonPath("$.partidas[?(@.nombreAlimento == 'Inicio B')].valorImpuestoCop")
                .value(org.hamcrest.Matchers.contains(3.0)));
    jdbc.update(
        "UPDATE recepciones_alimento SET nombre_alimento=NULL WHERE nombre_alimento='Inicio B'");
    mvc.perform(get(ruta("alimentos")).with(user("m3").roles("MODULO3")))
        .andExpect(
            jsonPath("$.partidas[?(@.datosCompletos == false)]")
                .value(org.hamcrest.Matchers.hasSize(1)));
  }

  @Test
  void medicamentoIncluyeNombreYPreciosSeparadosPorRecepcion() throws Exception {
    UUID enfermedad = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO enfermedades (id,nombre,nivel_riesgo,tipo,descripcion_clinica,
        requiere_sacrificio_sanitario,fecha_creacion,fecha_actualizacion)
        VALUES (?,'Prueba','BAJO','OTRA','Solo prueba',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
        """,
        enfermedad);
    UUID diagnostico = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO diagnosticos_galpon (id,galpon_id,active_galpon_id,lote_id,enfermedad_id,
        enfermedad_nombre,requiere_sacrificio_sanitario,observaciones,fecha_diagnostico,estado)
        VALUES (?,?,?,?,?,'Prueba',true,'Solo prueba',CURRENT_DATE,'ACTIVO')
        """,
        diagnostico,
        galpon,
        galpon,
        lote,
        enfermedad);
    UUID consumo = UUID.randomUUID();
    UUID medicamento = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO consumos_medicamento (id,diagnostico_id,galpon_id,lote_id,medicamento_id,
        cantidad_ingresada,unidad_ingresada,cantidad_unidad_base,unidad_base,fecha_aplicacion,
        fecha_registro,actor,idempotencia_clave)
        VALUES (?,?,?,?,?,5,'MILILITRO',5,'MILILITRO',CURRENT_DATE,CURRENT_TIMESTAMP,'prueba',?)
        """,
        consumo,
        diagnostico,
        galpon,
        lote,
        medicamento,
        UUID.randomUUID());
    for (int i = 1; i <= 2; i++) {
      UUID recepcion = UUID.randomUUID();
      jdbc.update(
          """
          INSERT INTO recepciones_medicamento (id,medicamento_id,codigo_lote,presentacion,
          fecha_ingreso,fecha_vencimiento,cantidad_presentaciones,contenido_por_presentacion,
          unidad_contenido,precio_neto_por_presentacion,impuesto_porcentaje,cantidad_unidad_base,
          unidad_base,precio_neto_unidad_base,precio_neto_total,valor_impuesto,precio_total,estado,
          actor,nombre_medicamento)
          VALUES (?,?,'P','Frasco',CURRENT_DATE,DATE '2099-01-01',1,10,'MILILITRO',100,19,10,
          'MILILITRO',10,100,19,119,'VIGENTE','prueba','Medicina prueba')
          """,
          recepcion,
          medicamento);
      jdbc.update(
          "INSERT INTO detalles_consumo_medicamento VALUES (?,?,?,?,?)",
          consumo,
          recepcion,
          i + 1,
          i * 10,
          "COP");
    }
    mvc.perform(get(ruta("medicamentos")).with(user("m3").roles("MODULO3")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partidas.length()").value(2))
        .andExpect(jsonPath("$.partidas[0].nombreMedicamento").value("Medicina prueba"))
        .andExpect(jsonPath("$.partidas[0].impuestoPorcentaje").value(19.0))
        .andExpect(
            jsonPath("$.partidas[*].precioNetoHistoricoUnidadBase")
                .value(org.hamcrest.Matchers.containsInAnyOrder(10.0, 20.0)))
        .andExpect(
            jsonPath("$.partidas[*].valorImpuestoCop")
                .value(org.hamcrest.Matchers.containsInAnyOrder(3.8, 11.4)))
        .andExpect(
            jsonPath("$.partidas[*].valorImpuestoRecepcionCop")
                .value(org.hamcrest.Matchers.contains(19.0, 19.0)));
  }

  @Test
  void avisoMinimoToleraCamposNuevosEsIdempotenteYValidaLote() throws Exception {
    UUID evento = UUID.randomUUID();
    String aviso =
        """
        {"eventId":"%s","eventType":"ResultadoSacrificioUtilizado","eventVersion":1,
         "aggregateId":"%s","campoNuevo":"ignorar",
         "payload":{"resultadoId":"%s","loteId":"%s","campoNuevo":true}}
        """
            .formatted(evento, resultado, resultado, lote);
    eventos.procesarKafka(aviso);
    eventos.procesarKafka(aviso);
    assertEquals(
        "UTILIZADO",
        jdbc.queryForObject(
            "SELECT estado FROM resultados_sacrificio_productivo WHERE id=?",
            String.class,
            resultado));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM confirmaciones_uso_resultado_sacrificio WHERE evento_id=?",
            Integer.class,
            evento));
    assertThrows(
        RuntimeException.class,
        () -> eventos.procesarKafka(aviso.replace(lote.toString(), UUID.randomUUID().toString())));
  }

  @Test
  void impuestoRedondeaMedioCentavoYRespetaExencion() throws Exception {
    UUID suministro = UUID.randomUUID();
    suministro(suministro, lote);
    tramoAlimento(suministro, "Fraccion", "1", "0.50", "1");
    tramoAlimento(suministro, "Exento", "2", "10", "0");
    mvc.perform(get(ruta("alimentos")).with(user("m3").roles("MODULO3")))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.partidas[?(@.nombreAlimento == 'Fraccion')].valorImpuestoCop")
                .value(org.hamcrest.Matchers.contains(0.01)))
        .andExpect(
            jsonPath("$.partidas[?(@.nombreAlimento == 'Exento')].valorImpuestoCop")
                .value(org.hamcrest.Matchers.contains(0.0)));
  }

  @Test
  void m3ConsultaProyeccionSinPoderCompletarSuCosto() throws Exception {
    jdbc.update(
        """
        INSERT INTO planes_nutricionales_lote (id,galpon_id,lote_id,codigo_lote,tipo_ave,
        fecha_ingreso,edad_ingreso_dias,poblacion_viva,fecha_salida_proyectada,estado)
        VALUES (?,?,?,'Lote prueba','POLLO',CURRENT_DATE,1,100,CURRENT_DATE,'ACTIVO')
        """,
        UUID.randomUUID(),
        galpon,
        lote);
    mvc.perform(
            get("/api/lotes/{id}/alimento-requerido", lote)
                .with(httpBasic("m3_test", "solo-pruebas-m3")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loteId").value(lote.toString()))
        .andExpect(jsonPath("$.etapas").isArray());
    mvc.perform(
            post("/api/proyecciones/{id}/completar-costo", UUID.randomUUID())
                .with(httpBasic("m3_test", "solo-pruebas-m3")))
        .andExpect(status().isForbidden());
  }

  private String ruta(String consulta) {
    return "/api/integracion/modulo3/lotes/" + lote + "/" + consulta;
  }

  private void suministro(UUID id, UUID loteId) {
    jdbc.update(
        """
        INSERT INTO suministros_diarios_alimento (id,galpon_id,lote_id,alimento_id,tipo_alimento,
        cantidad_kg,fecha_aplicacion,fecha_registro,trabajador,idempotencia_clave)
        VALUES (?,?,?,?,'INICIO',5,CURRENT_DATE,CURRENT_TIMESTAMP,'prueba',?)
        """,
        id,
        galpon,
        loteId,
        UUID.randomUUID(),
        UUID.randomUUID());
  }

  private void tramoAlimento(
      UUID suministro, String nombre, String cantidad, String precio, String impuesto) {
    UUID recepcion = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO recepciones_alimento (id,alimento_id,codigo_lote,fecha_ingreso,fecha_vencimiento,
        cantidad_bultos,peso_por_bulto_kg,precio_neto_por_bulto,impuesto_porcentaje,cantidad_total_kg,
        precio_neto_kg,precio_neto_total,valor_impuesto,precio_total,estado,actor,nombre_alimento,tipo_alimento)
        VALUES (?,?,'P',CURRENT_DATE,DATE '2099-01-01',1,10,100,?,10,999,100,19,119,'VIGENTE','prueba',?,'INICIO')
        """,
        recepcion,
        UUID.randomUUID(),
        new java.math.BigDecimal(impuesto),
        nombre);
    jdbc.update(
        "INSERT INTO detalles_suministro_alimento VALUES (?,?,?,?,?)",
        suministro,
        recepcion,
        new java.math.BigDecimal(cantidad),
        new java.math.BigDecimal(precio),
        "COP");
  }
}
