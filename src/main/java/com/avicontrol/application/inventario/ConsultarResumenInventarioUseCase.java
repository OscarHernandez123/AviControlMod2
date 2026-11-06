package com.avicontrol.application.inventario;

import com.avicontrol.domain.port.out.inventario.ResumenInventarioQueryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarResumenInventarioUseCase {
  private final ResumenInventarioQueryPort recepciones;
  private final BigDecimal capacidad;
  private final BigDecimal ocupacionConfigurada;

  public ConsultarResumenInventarioUseCase(
      ResumenInventarioQueryPort recepciones,
      @Value("${avicontrol.inventario.capacidad-maxima-kg:0}") BigDecimal capacidad,
      @Value("${avicontrol.inventario.ocupacion-actual-kg:-1}") BigDecimal ocupacionConfigurada) {
    this.recepciones = recepciones;
    this.capacidad = capacidad;
    this.ocupacionConfigurada = ocupacionConfigurada;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Resumen consultar() {
    BigDecimal ocupacion = ocupacionConfigurada.signum() < 0 ? null : ocupacionConfigurada;
    BigDecimal porcentaje =
        capacidad.signum() > 0 && ocupacion != null
            ? ocupacion.multiply(BigDecimal.valueOf(100)).divide(capacidad, 2, RoundingMode.HALF_UP)
            : null;
    return new Resumen(capacidad, ocupacion, porcentaje, recepciones.recientes());
  }

  public record Resumen(
      BigDecimal capacidadMaximaKg,
      BigDecimal ocupacionActualKg,
      BigDecimal porcentajeOcupacion,
      List<ResumenInventarioQueryPort.RecepcionReciente> recepcionesAlimentoRecientes) {}
}
