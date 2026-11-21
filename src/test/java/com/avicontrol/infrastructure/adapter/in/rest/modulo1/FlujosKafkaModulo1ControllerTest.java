package com.avicontrol.infrastructure.adapter.in.rest.modulo1;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.application.modulo1.eventos.FlujosKafkaModulo1UseCase;
import com.avicontrol.infrastructure.adapter.in.rest.error.ApiExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FlujosKafkaModulo1ControllerTest {
  @Mock FlujosKafkaModulo1UseCase caso;

  @Test
  void listaYConsultaOperacionesKafkaModulo1() throws Exception {
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new FlujosKafkaModulo1Controller(caso))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    UUID id = UUID.randomUUID();
    var res =
        new FlujosKafkaModulo1UseCase.Resultado(
            id,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "COMPLETADA",
            "EN_PRODUCCION",
            null,
            "MORTALIDAD");
    when(caso.listar()).thenReturn(List.of(res));
    when(caso.consultar(id)).thenReturn(res);

    mvc.perform(get("/api/integracion/modulo1/operaciones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id.toString()));

    mvc.perform(get("/api/integracion/modulo1/operaciones/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("COMPLETADA"));
  }
}
