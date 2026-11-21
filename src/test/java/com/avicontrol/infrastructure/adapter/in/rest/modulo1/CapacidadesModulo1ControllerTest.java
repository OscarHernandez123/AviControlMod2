package com.avicontrol.infrastructure.adapter.in.rest.modulo1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CapacidadesModulo1ControllerTest {
  @Autowired MockMvc mvc;

  @Test
  void retornaMapaCapacidadesConUsuarioAutenticadoY401SinAuth() throws Exception {
    mvc.perform(
            get("/api/integracion/modulo1/capacidades")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isMap());

    mvc.perform(get("/api/integracion/modulo1/capacidades"))
        .andExpect(status().isUnauthorized());
  }
}
