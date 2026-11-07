package com.avicontrol.domain.port.out.sacrificio.productivo;

import com.avicontrol.domain.model.sacrificio.productivo.ResultadoSacrificioProductivo;
import java.util.Optional;
import java.util.UUID;

public interface ResultadoSacrificioRepositoryPort {
  void crear(ResultadoSacrificioProductivo resultado);

  Optional<ResultadoSacrificioProductivo> buscarPorOrden(UUID ordenId);

  Optional<ResultadoSacrificioProductivo> buscar(UUID resultadoId);

  Optional<ResultadoSacrificioProductivo> bloquear(UUID resultadoId);

  boolean existePorLote(UUID loteId);

  boolean actualizarSiDisponible(ResultadoSacrificioProductivo resultado);

  boolean marcarUtilizado(UUID resultadoId);

  boolean registrarConfirmacion(UUID eventoId, UUID resultadoId);
}
