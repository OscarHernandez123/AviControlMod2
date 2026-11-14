package com.avicontrol.infrastructure.adapter.in.rest.recepcion;

import com.avicontrol.application.recepcion.ConsultarRecepcionesMedicamentoUseCase;
import com.avicontrol.application.recepcion.EditarRecepcionMedicamentoUseCase;
import com.avicontrol.application.recepcion.RegistrarRecepcionMedicamentoUseCase;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.EditarRecepcionMedicamentoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.HistorialMedicamentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RecepcionMedicamentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto.RegistrarRecepcionMedicamentoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.mapper.RecepcionFiltroRestMapper;
import com.avicontrol.infrastructure.adapter.in.rest.recepcion.mapper.RecepcionMedicamentoRestMapper;
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
@RequestMapping("/api/recepciones/medicamentos")
@SecurityRequirement(name = "basicAuth")
public class RecepcionMedicamentoController {
  private final RegistrarRecepcionMedicamentoUseCase useCase;
  private final ConsultarRecepcionesMedicamentoUseCase consulta;
  private final EditarRecepcionMedicamentoUseCase edicion;
  private final RecepcionMedicamentoRestMapper mapper;
  private final RecepcionFiltroRestMapper filtros;

  public RecepcionMedicamentoController(
      RegistrarRecepcionMedicamentoUseCase useCase,
      ConsultarRecepcionesMedicamentoUseCase consulta,
      EditarRecepcionMedicamentoUseCase edicion,
      RecepcionMedicamentoRestMapper mapper,
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
      summary = "Registrar recepción de medicamento",
      description =
          "Requiere ROLE_ADMINISTRADOR. Normaliza la cantidad a la unidad base y confirma la entrada de inventario en una transacción.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Recepción confirmada"),
    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
    @ApiResponse(responseCode = "401", description = "Sin autenticación"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
    @ApiResponse(responseCode = "404", description = "Medicamento no encontrado"),
    @ApiResponse(responseCode = "409", description = "Unidad incompatible o conflicto"),
    @ApiResponse(responseCode = "503", description = "Persistencia o inventario no disponible")
  })
  public ResponseEntity<RecepcionMedicamentoResponse> registrar(
      @Valid @RequestBody RegistrarRecepcionMedicamentoRequest request, Authentication actor) {
    RecepcionMedicamento r = useCase.registrar(mapper.command(request, actor.getName()));
    return ResponseEntity.created(URI.create("/api/recepciones/medicamentos/" + r.id()))
        .body(mapper.response(r));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Historial de recepciones de medicamento",
      description =
          "Busca por lote o nombre histórico; filtra por presentación y fecha de ingreso. Sin fechas usa los últimos 30 días.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página e indicador mensual"),
    @ApiResponse(responseCode = "400", description = "Filtros inválidos"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
  })
  public HistorialMedicamentoResponse listar(
      @RequestParam(required = false) String busqueda,
      @RequestParam(required = false) String presentacion,
      @RequestParam(required = false) EstadoRecepcion estado,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamano) {
    return mapper.historial(
        consulta.listar(
            filtros.crear(busqueda, estado, desde, hasta, presentacion, pagina, tamano)));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Detalle de una recepción de medicamento")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detalle histórico"),
    @ApiResponse(responseCode = "404", description = "Recepción no encontrada")
  })
  public RecepcionMedicamentoResponse detalle(@PathVariable UUID id) {
    return mapper.response(consulta.detalle(id));
  }

  @GetMapping("/{id}/edicion")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Campos editables de una recepción de medicamento",
      description = "Devuelve 409 si hay movimientos posteriores confirmados.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Siete campos editables"),
    @ApiResponse(responseCode = "404", description = "Recepción no encontrada"),
    @ApiResponse(responseCode = "409", description = "Recepción utilizada")
  })
  public EditarRecepcionMedicamentoRequest formularioEdicion(@PathVariable UUID id) {
    return mapper.formulario(consulta.prepararEdicion(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Editar recepción de medicamento",
      description =
          "Actualiza solo siete campos, la entrada inicial y la auditoría en una transacción.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Recepción actualizada"),
    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
    @ApiResponse(responseCode = "404", description = "Recepción o medicamento no encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "Recepción utilizada, vencimiento o unidad incompatible"),
    @ApiResponse(responseCode = "503", description = "Persistencia o Inventario no disponible")
  })
  public RecepcionMedicamentoResponse editar(
      @PathVariable UUID id,
      @Valid @RequestBody EditarRecepcionMedicamentoRequest request,
      Authentication actor) {
    return mapper.response(edicion.editar(mapper.command(id, request, actor.getName())));
  }
}
