package com.avicontrol.application.inventario;

import com.avicontrol.domain.model.inventario.ExistenciaAlimento;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarCoberturaAlimentoUseCase {
  private final ConsultarInventarioUseCase inventario;

  public ConsultarCoberturaAlimentoUseCase(ConsultarInventarioUseCase inventario) {
    this.inventario = inventario;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Cobertura consultar(UUID alimentoId) {
    ExistenciaAlimento alimento =
        inventario.consultar().alimentos().stream()
            .filter(a -> a.alimentoId().equals(alimentoId))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Alimento no encontrado"));
    return new Cobertura(
        alimentoId,
        alimento.alimento(),
        alimento.etapaCrianza(),
        alimento.stockActualKg(),
        alimento.demandaKg(),
        alimento.estadoCobertura());
  }

  public record Cobertura(
      UUID alimentoId,
      String alimento,
      String etapaCrianza,
      BigDecimal stockActualKg,
      BigDecimal demandaKg,
      String estado) {}
}
