package com.avicontrol.infrastructure.adapter.in.rest.modulo1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.application.modulo1.eventos.SolicitarEventoModulo1UseCase;
import com.avicontrol.domain.port.out.modulo1.EventosModulo1Port;
import com.avicontrol.infrastructure.adapter.in.rest.error.ApiExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EventosModulo1ControllerTest {
  @Mock SolicitarEventoModulo1UseCase caso;
  @Mock EventosModulo1Port eventos;

  @Test
  void aceptaEventosValidosRechazaCantidadDecimalYConsultaEstado() throws Exception {
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new EventosModulo1Controller(caso, eventos))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    UUID id = UUID.randomUUID();
    EventosModulo1Port.Entrega entrega = new EventosModulo1Port.Entrega(id, "PENDIENTE", 0);
    when(caso.solicitar(eq(SolicitarEventoModulo1UseCase.Tipo.MORTALIDAD), any()))
        .thenReturn(entrega);
    when(eventos.consultar(id)).thenReturn(entrega);

    mvc.perform(
            post("/api/integracion/modulo1/eventos/mortalidad")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"alertaId\":\""
                        + id
                        + "\",\"galponId\":\""
                        + UUID.randomUUID()
                        + "\",\"fechaHoraEvento\":\"2026-11-20T10:00:00-05:00\",\"cantidadMuertos\":2}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.alertaId").value(id.toString()));

    mvc.perform(
            post("/api/integracion/modulo1/eventos/mortalidad")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"galponId\":\"" + UUID.randomUUID() + "\",\"cantidadMuertos\":2.5}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));

    mvc.perform(get("/api/integracion/modulo1/eventos/{alertaId}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("PENDIENTE"));
  }
}
