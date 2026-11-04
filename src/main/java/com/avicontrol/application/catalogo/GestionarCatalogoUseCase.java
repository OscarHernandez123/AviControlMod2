package com.avicontrol.application.catalogo;

import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.ResourceNotFoundException;
import com.avicontrol.domain.model.catalogo.*;
import com.avicontrol.domain.port.out.catalogo.AlimentoRepositoryPort;
import com.avicontrol.domain.port.out.catalogo.MedicamentoRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class GestionarCatalogoUseCase {
  private final AlimentoRepositoryPort alimentos;
  private final MedicamentoRepositoryPort medicamentos;
  private final CurrentActorProvider actor;

  public GestionarCatalogoUseCase(
      AlimentoRepositoryPort alimentos,
      MedicamentoRepositoryPort medicamentos,
      CurrentActorProvider actor) {
    this.alimentos = alimentos;
    this.medicamentos = medicamentos;
    this.actor = actor;
  }

  @Transactional
  public Alimento registrarAlimento(AlimentoCommand c) {
    CatalogoAuthorization.requireAdministrador(actor);
    return alimentos.guardar(
        new Alimento(
            UUID.randomUUID(),
            c.nombreComercial(),
            c.marca(),
            c.tipo(),
            c.pesoNominalBultoKg(),
            c.duracionRecomendadaEtapaDias(),
            c.estado()));
  }

  @Transactional
  public Alimento editarAlimento(UUID id, AlimentoCommand c) {
    CatalogoAuthorization.requireAdministrador(actor);
    buscarAlimento(id);
    return alimentos.guardar(
        new Alimento(
            id,
            c.nombreComercial(),
            c.marca(),
            c.tipo(),
            c.pesoNominalBultoKg(),
            c.duracionRecomendadaEtapaDias(),
            c.estado()));
  }

  @Transactional(readOnly = true)
  public Alimento consultarAlimento(UUID id) {
    CatalogoAuthorization.requireAdministrador(actor);
    return buscarAlimento(id);
  }

  @Transactional(readOnly = true)
  public List<Alimento> listarAlimentos() {
    CatalogoAuthorization.requireAdministrador(actor);
    return alimentos.listar();
  }

  @Transactional
  public Medicamento registrarMedicamento(MedicamentoCommand c) {
    CatalogoAuthorization.requireAdministrador(actor);
    return medicamentos.guardar(
        new Medicamento(
            UUID.randomUUID(),
            c.nombreComercial(),
            c.presentacionCompatible(),
            c.unidadBase(),
            c.estado()));
  }

  @Transactional
  public Medicamento editarMedicamento(UUID id, MedicamentoCommand c) {
    CatalogoAuthorization.requireAdministrador(actor);
    buscarMedicamento(id);
    return medicamentos.guardar(
        new Medicamento(
            id, c.nombreComercial(), c.presentacionCompatible(), c.unidadBase(), c.estado()));
  }

  @Transactional(readOnly = true)
  public Medicamento consultarMedicamento(UUID id) {
    CatalogoAuthorization.requireAdministrador(actor);
    return buscarMedicamento(id);
  }

  @Transactional(readOnly = true)
  public List<Medicamento> listarMedicamentos() {
    CatalogoAuthorization.requireAdministrador(actor);
    return medicamentos.listar();
  }

  private Alimento buscarAlimento(UUID id) {
    return alimentos
        .buscarPorId(id)
        .orElseThrow(() -> new ResourceNotFoundException("No existe el alimento solicitado"));
  }

  private Medicamento buscarMedicamento(UUID id) {
    return medicamentos
        .buscarPorId(id)
        .orElseThrow(() -> new ResourceNotFoundException("No existe el medicamento solicitado"));
  }
}
