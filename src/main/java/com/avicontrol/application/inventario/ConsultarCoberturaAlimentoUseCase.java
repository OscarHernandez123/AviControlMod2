package com.avicontrol.application.inventario;

import com.avicontrol.domain.model.inventario.ExistenciaAlimento;
import com.avicontrol.domain.port.out.inventario.DemandaAlimentoQueryPort;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarCoberturaAlimentoUseCase {
  private final ConsultarInventarioUseCase inventario;
  private final DemandaAlimentoQueryPort demanda;

  public ConsultarCoberturaAlimentoUseCase(
      ConsultarInventarioUseCase inventario, DemandaAlimentoQueryPort demanda) {
    this.inventario = inventario;
    this.demanda = demanda;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Cobertura consultar(UUID alimentoId) {
    ExistenciaAlimento alimento =
        inventario.consultar().alimentos().stream()
            .filter(a -> a.alimentoId().equals(alimentoId))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Alimento no encontrado"));
    BigDecimal kg =
        demanda.demandaVigente(alimentoId, alimento.alimento(), alimento.etapaCrianza());
    String estado =
        kg.signum() == 0
            ? "SIN_REQUERIMIENTO"
            : alimento.stockActualKg().signum() == 0
                ? "SIN_COBERTURA"
                : alimento.stockActualKg().compareTo(kg) < 0 ? "COBERTURA_PARCIAL" : "CUMPLE";
    return new Cobertura(
        alimentoId,
        alimento.alimento(),
        alimento.etapaCrianza(),
        alimento.stockActualKg(),
        kg,
        estado);
  }

  public record Cobertura(
      UUID alimentoId,
      String alimento,
      String etapaCrianza,
      BigDecimal stockActualKg,
      BigDecimal demandaKg,
      String estado) {}
}
