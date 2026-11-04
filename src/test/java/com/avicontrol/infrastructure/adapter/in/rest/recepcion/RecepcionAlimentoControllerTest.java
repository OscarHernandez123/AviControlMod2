package com.avicontrol.infrastructure.adapter.in.rest.recepcion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.avicontrol.application.catalogo.port.*;
import com.avicontrol.application.recepcion.GestionarRecepcionesUseCase;
import com.avicontrol.application.support.*;
import com.avicontrol.domain.model.catalogo.*;
import com.avicontrol.domain.port.out.recepcion.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecepcionAlimentoControllerTest {
  @Test
  void registraYPaginaElHistorial() throws Exception {
    RecepcionAlimentoRepositoryPort repo = mock();
    AlimentoCatalogoQueryPort catalogo = mock();
    UUID alimentoId = UUID.randomUUID();
    when(catalogo.buscarAlimentoActivo(alimentoId))
        .thenReturn(
            Optional.of(
                new Alimento(
                    alimentoId,
                    "Inicio",
                    "Marca",
                    TipoAlimento.INICIO,
                    BigDecimal.TEN,
                    null,
                    EstadoProductoCatalogo.ACTIVO)));
    when(repo.guardar(any())).thenAnswer(i -> i.getArgument(0));
    when(repo.listar()).thenReturn(List.of());
    CurrentActorProvider actor = () -> new CurrentActor("admin", Set.of("ROLE_ADMINISTRADOR"));
    GestionarRecepcionesUseCase useCase =
        new GestionarRecepcionesUseCase(
            repo,
            mock(RecepcionMedicamentoRepositoryPort.class),
            catalogo,
            mock(MedicamentoCatalogoQueryPort.class),
            mock(InventarioRecepcionPort.class),
            mock(AuditoriaRecepcionRepositoryPort.class),
            actor,
            Clock.systemUTC(),
            mock(RecepcionEventPublisherPort.class));
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new RecepcionAlimentoController(useCase)).build();
    String body =
        "{\"alimentoId\":\""
            + alimentoId
            + "\",\"tipoAlimento\":\"INICIO\",\"codigoLote\":\"L-1\",\"fechaIngreso\":\"2026-01-01\",\"fechaVencimiento\":\"2026-02-01\",\"cantidadBultos\":2,\"pesoPorBultoKg\":40,\"precioNetoPorBulto\":125000,\"porcentajeImpuesto\":5}";

    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigoLote").value("L-1"));
    mvc.perform(get("/api/recepciones/alimentos?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }
}
