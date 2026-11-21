package com.avicontrol.infrastructure.adapter.in.rest.recepcion;

import com.avicontrol.application.recepcion.ConsultarRecepcionesAlimentoUseCase;
import com.avicontrol.application.recepcion.EditarRecepcionAlimentoUseCase;
import com.avicontrol.application.recepcion.RegistrarRecepcionAlimentoUseCase;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.EditarRecepcionAlimentoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.HistorialAlimentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RecepcionAlimentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RegistrarRecepcionAlimentoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.mapper.RecepcionAlimentoRestMapper;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.mapper.RecepcionFiltroRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recepciones/alimentos")
@SecurityRequirement(name = "basicAuth")
public class RecepcionAlimentoController {
  private final RegistrarRecepcionAlimentoUseCase useCase;
  private final ConsultarRecepcionesAlimentoUseCase consulta;
  private final EditarRecepcionAlimentoUseCase edicion;
  private final RecepcionAlimentoRestMapper mapper;
  private final RecepcionFiltroRestMapper filtros;

  public RecepcionAlimentoController(
      RegistrarRecepcionAlimentoUseCase useCase,
      ConsultarRecepcionesAlimentoUseCase consulta,
      EditarRecepcionAlimentoUseCase edicion,
      RecepcionAlimentoRestMapper mapper,
      RecepcionFiltroRestMapper filtros) {
    this.useCase = useCase;
    this.consulta = consulta;
    this.edicion = edicion;
    this.mapper = mapper;
    this.filtros = filtros;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Registrar recepción de alimento",
      description =
          "Requiere ROLE_ADMINISTRADOR. Confirma la recepción y la entrada inicial en kg en una transacción.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Recepción confirmada"),
    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
    @ApiResponse(responseCode = "401", description = "Sin autenticación"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
    @ApiResponse(responseCode = "404", description = "Alimento no encontrado"),
    @ApiResponse(responseCode = "409", description = "Conflicto de recepción"),
    @ApiResponse(responseCode = "503", description = "Persistencia o inventario no disponible")
  })
  public ResponseEntity<RecepcionAlimentoResponse> registrar(
      @Valid @RequestBody RegistrarRecepcionAlimentoRequest request, Authentication actor) {
    RecepcionAlimento r = useCase.registrar(mapper.command(request, actor.getName()));
    return ResponseEntity.created(URI.create("/api/recepciones/alimentos/" + r.id()))
        .body(mapper.response(r));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Historial de recepciones de alimento",
      description =
          "Busca por ID o lote; filtra por estado y fecha de ingreso. Sin fechas usa los últimos 30 días.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página e indicadores"),
    @ApiResponse(responseCode = "400", description = "Filtros inválidos"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
  })
  public HistorialAlimentoResponse listar(
      @RequestParam(required = false) String busqueda,
      @RequestParam(required = false) EstadoRecepcion estado,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamano) {
    return mapper.historial(
        consulta.listar(filtros.crear(busqueda, estado, desde, hasta, null, pagina, tamano)));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Detalle de una recepción de alimento")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detalle histórico"),
    @ApiResponse(responseCode = "404", description = "Recepción no encontrada")
  })
  public RecepcionAlimentoResponse detalle(@PathVariable UUID id) {
    return mapper.response(consulta.detalle(id));
  }

  @GetMapping("/{id}/edicion")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Campos editables de una recepción de alimento",
      description = "Devuelve 409 si hay movimientos posteriores confirmados.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Seis campos editables"),
    @ApiResponse(responseCode = "404", description = "Recepción no encontrada"),
    @ApiResponse(responseCode = "409", description = "Recepción utilizada")
  })
  public EditarRecepcionAlimentoRequest formularioEdicion(@PathVariable UUID id) {
    return mapper.formulario(consulta.prepararEdicion(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Editar recepción de alimento",
      description =
          "Actualiza solo seis campos, la entrada inicial y la auditoría en una transacción.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Recepción actualizada"),
    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
    @ApiResponse(responseCode = "404", description = "Recepción o alimento no encontrado"),
    @ApiResponse(responseCode = "409", description = "Recepción utilizada o vencimiento inválido"),
    @ApiResponse(responseCode = "503", description = "Persistencia o Inventario no disponible")
  })
  public RecepcionAlimentoResponse editar(
      @PathVariable UUID id,
      @Valid @RequestBody EditarRecepcionAlimentoRequest request,
      Authentication actor) {
    return mapper.response(edicion.editar(mapper.command(id, request, actor.getName())));
  }
}
