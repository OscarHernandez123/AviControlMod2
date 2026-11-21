package com.avicontrol.infrastructure.adapter.in.rest.modulo3;

import com.avicontrol.application.sacrificio.productivo.ConsultarResultadoSacrificioUseCase;
import com.avicontrol.domain.port.out.modulo3.FinanzasLoteQueryPort;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ResultadoSacrificioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.mapper.ResultadoSacrificioRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integracion/modulo3/lotes/{loteId}")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','MODULO3')")
@SecurityRequirement(name = "basicAuth")
@Transactional(readOnly = true)
public class ConsultasModulo3Controller {
  private final FinanzasLoteQueryPort partidas;
  private final ConsultarResultadoSacrificioUseCase resultados;
  private final ResultadoSacrificioRestMapper mapper;

  public ConsultasModulo3Controller(
      FinanzasLoteQueryPort partidas,
      ConsultarResultadoSacrificioUseCase resultados,
      ResultadoSacrificioRestMapper mapper) {
    this.partidas = partidas;
    this.resultados = resultados;
    this.mapper = mapper;
  }

  @GetMapping("/resultado-sacrificio")
  @Operation(summary = "Consultar resultado final por lote para Módulo 3")
  public ResultadoSacrificioResponse resultado(@PathVariable UUID loteId) {
    return mapper.response(resultados.consultarPorLote(loteId));
  }

  @GetMapping("/alimentos")
  @Operation(summary = "Consultar suministros reales por tramo de recepción; no es una proyección")
  public AlimentosLote alimentos(@PathVariable UUID loteId) {
    return new AlimentosLote(loteId, partidas.alimentos(loteId));
  }

  @GetMapping("/medicamentos")
  @Operation(summary = "Consultar medicamentos aplicados con nombre, precio histórico e impuesto")
  public MedicamentosLote medicamentos(@PathVariable UUID loteId) {
    return new MedicamentosLote(loteId, partidas.medicamentos(loteId));
  }

  public record AlimentosLote(UUID loteId, List<FinanzasLoteQueryPort.PartidaAlimento> partidas) {}

  public record MedicamentosLote(
      UUID loteId, List<FinanzasLoteQueryPort.PartidaMedicamento> partidas) {}
}
