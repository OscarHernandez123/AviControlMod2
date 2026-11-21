package com.avicontrol.application.inventario;

import com.avicontrol.domain.model.inventario.ExistenciaAlimento;
import com.avicontrol.domain.model.inventario.InventarioActual;
import com.avicontrol.domain.port.out.inventario.DemandaAlimentoQueryPort;
import com.avicontrol.domain.port.out.inventario.InventarioMovimientoQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarInventarioUseCase {
  private final InventarioMovimientoQueryPort movimientos;
  private final DemandaAlimentoQueryPort demanda;
  private final Clock clock;

  public ConsultarInventarioUseCase(
      InventarioMovimientoQueryPort movimientos, DemandaAlimentoQueryPort demanda, Clock clock) {
    this.movimientos = movimientos;
    this.demanda = demanda;
    this.clock = clock;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public InventarioActual consultar() {
    InventarioActual actual = movimientos.consultar(LocalDate.now(clock));
    var alimentos =
        actual.alimentos().stream()
            .map(
                a -> {
                  var kg = demanda.demandaVigente(a.alimentoId(), a.alimento(), a.etapaCrianza());
                  String estado =
                      kg.signum() == 0
                          ? "SIN_REQUERIMIENTO"
                          : a.stockActualKg().signum() == 0
                              ? "SIN_COBERTURA"
                              : a.stockActualKg().compareTo(kg) < 0
                                  ? "COBERTURA_PARCIAL"
                                  : "CUMPLE";
                  return new ExistenciaAlimento(
                      a.alimentoId(),
                      a.alimento(),
                      a.etapaCrianza(),
                      a.recepcionesActivas(),
                      a.proximoVencimiento(),
                      a.stockActualKg(),
                      kg,
                      estado);
                })
            .toList();
    return new InventarioActual(
        alimentos,
        actual.medicamentos(),
        actual.tarjetasAlimentoKg(),
        actual.tarjetasMedicamento());
  }
}
