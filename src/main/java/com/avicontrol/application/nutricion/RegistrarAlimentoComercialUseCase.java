package com.avicontrol.application.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.port.out.nutricion.AlimentoComercialRepositoryPort;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarAlimentoComercialUseCase {
  private final AlimentoComercialRepositoryPort repositorio;
  private final AlimentoCompatibleQueryPort consulta;

  public RegistrarAlimentoComercialUseCase(
      AlimentoComercialRepositoryPort repositorio, AlimentoCompatibleQueryPort consulta) {
    this.repositorio = repositorio;
    this.consulta = consulta;
  }

  @Transactional
  public AlimentoCompatibleQueryPort.Alimento registrar(Datos datos) {
    if (datos == null
        || datos.id() == null
        || datos.nombre() == null
        || datos.nombre().isBlank()
        || datos.tipoAlimento() == null
        || datos.pesoNominalPorBultoKg() == null
        || datos.pesoNominalPorBultoKg().signum() <= 0
        || datos.proteinaPorcentaje() == null
        || datos.proteinaPorcentaje().signum() < 0
        || datos.proteinaPorcentaje().compareTo(BigDecimal.valueOf(100)) > 0
        || datos.energiaKcalKg() == null
        || datos.energiaKcalKg().signum() <= 0
        || (datos.duracionRecomendadaEtapaDias() != null
            && datos.duracionRecomendadaEtapaDias() <= 0))
      throw new IllegalArgumentException("Datos del alimento comercial inválidos");
    repositorio.registrar(
        datos.id(),
        datos.nombre().strip(),
        datos.tipoAlimento(),
        datos.pesoNominalPorBultoKg(),
        datos.proteinaPorcentaje(),
        datos.energiaKcalKg(),
        datos.duracionRecomendadaEtapaDias());
    return consulta.buscar(datos.id()).orElseThrow();
  }

  public record Datos(
      UUID id,
      String nombre,
      EtapaCrianza tipoAlimento,
      BigDecimal pesoNominalPorBultoKg,
      BigDecimal proteinaPorcentaje,
      BigDecimal energiaKcalKg,
      Integer duracionRecomendadaEtapaDias) {}
}
