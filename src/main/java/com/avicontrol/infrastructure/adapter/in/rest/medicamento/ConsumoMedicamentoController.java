package com.avicontrol.infrastructure.adapter.in.rest.medicamento;

import com.avicontrol.application.medicamento.RegistrarConsumoMedicamentoUseCase;
import com.avicontrol.application.medicamento.result.ConsumoMedicamentoResult;
import com.avicontrol.domain.exception.medicamento.ConsumoMedicamentoNoEncontradoException;
import com.avicontrol.domain.port.out.medicamento.ConsumoMedicamentoRepositoryPort;
import com.avicontrol.infrastructure.adapter.in.rest.medicamento.dto.ConsumoMedicamentoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicamento.dto.RegistrarConsumoMedicamentoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.medicamento.mapper.ConsumoMedicamentoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "basicAuth")
public class ConsumoMedicamentoController {
  private final RegistrarConsumoMedicamentoUseCase registro;
  private final ConsumoMedicamentoRepositoryPort consumos;
  private final ConsumoMedicamentoRestMapper mapper;

  public ConsumoMedicamentoController(
      RegistrarConsumoMedicamentoUseCase registro,
      ConsumoMedicamentoRepositoryPort consumos,
      ConsumoMedicamentoRestMapper mapper) {
    this.registro = registro;
    this.consumos = consumos;
    this.mapper = mapper;
  }

  @PostMapping("/diagnosticos/{diagnosticoId}/consumos-medicamento")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(
      summary = "Registrar consumo de medicamento",
      description =
          "Descuenta FEFO y conserva el precio histórico de cada recepción. Requiere Idempotency-Key UUID.")
  public ResponseEntity<ConsumoMedicamentoResponse> registrar(
      @PathVariable UUID diagnosticoId,
      @RequestHeader("Idempotency-Key") UUID clave,
      @Valid @RequestBody RegistrarConsumoMedicamentoRequest request,
      Authentication actor) {
    ConsumoMedicamentoResponse response =
        mapper.response(
            registro.registrar(mapper.command(diagnosticoId, request, actor.getName(), clave)));
    return ResponseEntity.created(URI.create("/api/consumos-medicamento/" + response.id()))
        .body(response);
  }

  @GetMapping("/consumos-medicamento/{consumoId}")
  @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMINISTRADOR')")
  @Operation(summary = "Consultar consumo y costos históricos por recepción")
  public ConsumoMedicamentoResponse detalle(@PathVariable UUID consumoId) {
    return mapper.response(
        ConsumoMedicamentoResult.de(
            consumos.buscar(consumoId).orElseThrow(ConsumoMedicamentoNoEncontradoException::new)));
  }

  @GetMapping("/lotes/{loteId}/consumos-medicamento")
  @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMINISTRADOR')")
  @Operation(summary = "Consultar consumos y costos históricos de un lote")
  public List<ConsumoMedicamentoResponse> porLote(@PathVariable UUID loteId) {
    return consumos.listarPorLote(loteId).stream()
        .map(ConsumoMedicamentoResult::de)
        .map(mapper::response)
        .toList();
  }
}
