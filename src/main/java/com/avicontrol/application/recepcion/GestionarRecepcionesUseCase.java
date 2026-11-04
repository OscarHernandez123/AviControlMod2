package com.avicontrol.application.recepcion;

import com.avicontrol.application.catalogo.port.AlimentoCatalogoQueryPort;
import com.avicontrol.application.catalogo.port.MedicamentoCatalogoQueryPort;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.BusinessRuleViolationException;
import com.avicontrol.domain.exception.ResourceNotFoundException;
import com.avicontrol.domain.exception.recepcion.RecepcionUtilizadaException;
import com.avicontrol.domain.model.catalogo.Alimento;
import com.avicontrol.domain.model.catalogo.Medicamento;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.domain.model.recepcion.RegistroAuditoriaRecepcion;
import com.avicontrol.domain.port.out.recepcion.AuditoriaRecepcionRepositoryPort;
import com.avicontrol.domain.port.out.recepcion.InventarioRecepcionPort;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
import com.avicontrol.domain.port.out.recepcion.RecepcionEventPublisherPort;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.transaction.annotation.Transactional;

public class GestionarRecepcionesUseCase {
  private final RecepcionAlimentoRepositoryPort alimentos;
  private final RecepcionMedicamentoRepositoryPort medicamentos;
  private final AlimentoCatalogoQueryPort catalogoAlimentos;
  private final MedicamentoCatalogoQueryPort catalogoMedicamentos;
  private final InventarioRecepcionPort inventario;
  private final AuditoriaRecepcionRepositoryPort auditoria;
  private final CurrentActorProvider actor;
  private final Clock clock;
  private final RecepcionEventPublisherPort events;

  public GestionarRecepcionesUseCase(
      RecepcionAlimentoRepositoryPort alimentos,
      RecepcionMedicamentoRepositoryPort medicamentos,
      AlimentoCatalogoQueryPort catalogoAlimentos,
      MedicamentoCatalogoQueryPort catalogoMedicamentos,
      InventarioRecepcionPort inventario,
      AuditoriaRecepcionRepositoryPort auditoria,
      CurrentActorProvider actor,
      Clock clock,
      RecepcionEventPublisherPort events) {
    this.alimentos = alimentos;
    this.medicamentos = medicamentos;
    this.catalogoAlimentos = catalogoAlimentos;
    this.catalogoMedicamentos = catalogoMedicamentos;
    this.inventario = inventario;
    this.auditoria = auditoria;
    this.actor = actor;
    this.clock = clock;
    this.events = events;
  }

  @Transactional
  public RecepcionAlimento registrarAlimento(RecepcionAlimentoCommand c) {
    authorize();
    Alimento producto =
        catalogoAlimentos
            .buscarAlimentoActivo(c.alimentoId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No existe un alimento activo con el identificador indicado"));
    if (producto.tipo() != c.tipoAlimento())
      throw new BusinessRuleViolationException(
          "TIPO_ALIMENTO_INVALIDO", "El tipo no coincide con el alimento seleccionado");
    RecepcionAlimento r =
        new RecepcionAlimento(
            UUID.randomUUID(),
            producto.id(),
            producto.nombreComercial(),
            producto.tipo(),
            c.codigoLote(),
            c.fechaIngreso(),
            c.fechaVencimiento(),
            c.cantidadBultos(),
            c.pesoPorBultoKg(),
            c.precioNetoPorBulto(),
            c.porcentajeImpuesto(),
            EstadoRecepcion.CONFIRMADA);
    r = alimentos.guardar(r);
    inventario.registrarAlimento(r.id(), r.pesoTotalKg());
    events.publish(new RecepcionAlimentoConfirmadaV1(r));
    return r;
  }

  @Transactional
  public RecepcionMedicamento registrarMedicamento(RecepcionMedicamentoCommand c) {
    authorize();
    Medicamento producto =
        catalogoMedicamentos
            .buscarMedicamentoActivo(c.medicamentoId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No existe un medicamento activo con el identificador indicado"));
    if (producto.unidadBase() != c.unidadMedida()
        && !(producto.unidadBase().name().equals("GRAMO")
            && c.unidadMedida().name().equals("MILIGRAMO")))
      throw new BusinessRuleViolationException(
          "UNIDAD_INCOMPATIBLE", "La unidad no es compatible con el medicamento seleccionado");
    RecepcionMedicamento r =
        new RecepcionMedicamento(
            UUID.randomUUID(),
            producto.id(),
            producto.nombreComercial(),
            c.codigoLote(),
            c.presentacion(),
            c.cantidad(),
            c.contenidoPorPresentacion(),
            c.unidadMedida(),
            c.fechaIngreso(),
            c.fechaVencimiento(),
            c.precioNetoPorPresentacion(),
            c.porcentajeImpuesto(),
            EstadoRecepcion.CONFIRMADA);
    r = medicamentos.guardar(r);
    inventario.registrarMedicamento(r.id(), r.contenidoTotalBase(), r.unidadBase().name());
    events.publish(new RecepcionMedicamentoConfirmadaV1(r));
    return r;
  }

  @Transactional
  public RecepcionAlimento editarAlimento(UUID id, RecepcionAlimentoCommand c) {
    authorize();
    RecepcionAlimento anterior = detalleAlimento(id);
    if (inventario.alimentoUtilizado(id)) throw new RecepcionUtilizadaException();
    Alimento producto =
        catalogoAlimentos
            .buscarAlimentoActivo(c.alimentoId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No existe un alimento activo con el identificador indicado"));
    if (producto.tipo() != anterior.tipoAlimentoHistorico())
      throw new BusinessRuleViolationException(
          "TIPO_ALIMENTO_INVALIDO", "El alimento debe conservar el tipo histórico");
    RecepcionAlimento actual =
        new RecepcionAlimento(
            id,
            producto.id(),
            producto.nombreComercial(),
            anterior.tipoAlimentoHistorico(),
            anterior.codigoLote(),
            anterior.fechaIngreso(),
            c.fechaVencimiento(),
            c.cantidadBultos(),
            c.pesoPorBultoKg(),
            c.precioNetoPorBulto(),
            c.porcentajeImpuesto(),
            EstadoRecepcion.CONFIRMADA);
    actual = alimentos.guardar(actual);
    inventario.ajustarAlimento(id, actual.pesoTotalKg());
    auditar(id, anterior, actual);
    events.publish(new RecepcionAlimentoActualizadaV1(actual));
    return actual;
  }

  @Transactional
  public RecepcionMedicamento editarMedicamento(UUID id, RecepcionMedicamentoCommand c) {
    authorize();
    RecepcionMedicamento anterior = detalleMedicamento(id);
    if (inventario.medicamentoUtilizado(id)) throw new RecepcionUtilizadaException();
    Medicamento producto =
        catalogoMedicamentos
            .buscarMedicamentoActivo(c.medicamentoId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No existe un medicamento activo con el identificador indicado"));
    if (producto.unidadBase() != c.unidadMedida()
        && !(producto.unidadBase().name().equals("GRAMO")
            && c.unidadMedida().name().equals("MILIGRAMO")))
      throw new BusinessRuleViolationException(
          "UNIDAD_INCOMPATIBLE", "La unidad no es compatible con el medicamento seleccionado");
    RecepcionMedicamento actual =
        new RecepcionMedicamento(
            id,
            producto.id(),
            producto.nombreComercial(),
            anterior.codigoLote(),
            anterior.presentacion(),
            c.cantidad(),
            c.contenidoPorPresentacion(),
            c.unidadMedida(),
            anterior.fechaIngreso(),
            c.fechaVencimiento(),
            c.precioNetoPorPresentacion(),
            c.porcentajeImpuesto(),
            EstadoRecepcion.CONFIRMADA);
    actual = medicamentos.guardar(actual);
    inventario.ajustarMedicamento(id, actual.contenidoTotalBase(), actual.unidadBase().name());
    auditar(id, anterior, actual);
    events.publish(new RecepcionMedicamentoActualizadaV1(actual));
    return actual;
  }

  @Transactional(readOnly = true)
  public RecepcionAlimento detalleAlimento(UUID id) {
    authorize();
    return alimentos
        .buscarPorId(id)
        .orElseThrow(() -> new ResourceNotFoundException("No existe la recepción de alimento"));
  }

  @Transactional(readOnly = true)
  public RecepcionMedicamento detalleMedicamento(UUID id) {
    authorize();
    return medicamentos
        .buscarMedicamentoPorId(id)
        .orElseThrow(() -> new ResourceNotFoundException("No existe la recepción de medicamento"));
  }

  @Transactional(readOnly = true)
  public List<RecepcionAlimento> listarAlimentos() {
    authorize();
    return alimentos.listar();
  }

  @Transactional(readOnly = true)
  public List<RecepcionMedicamento> listarMedicamentos() {
    authorize();
    return medicamentos.listarMedicamentos();
  }

  @Transactional(readOnly = true)
  public PaginaResultado<RecepcionAlimento> buscarAlimentos(FiltroRecepcionAlimento filtro) {
    authorize();
    Stream<RecepcionAlimento> stream = alimentos.listar().stream();
    if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
      String term = filtro.busqueda().toLowerCase();
      stream =
          stream.filter(
              r ->
                  r.codigoLote().toLowerCase().contains(term)
                      || r.nombreAlimentoHistorico().toLowerCase().contains(term)
                      || r.id().toString().contains(term));
    }
    if (filtro.estado() != null) stream = stream.filter(r -> r.estado() == filtro.estado());
    if (filtro.desde() != null)
      stream = stream.filter(r -> !r.fechaIngreso().isBefore(filtro.desde()));
    if (filtro.hasta() != null)
      stream = stream.filter(r -> !r.fechaIngreso().isAfter(filtro.hasta()));
    return pagina(
        stream.sorted(Comparator.comparing(RecepcionAlimento::fechaIngreso).reversed()).toList(),
        filtro.page(),
        filtro.size());
  }

  @Transactional(readOnly = true)
  public PaginaResultado<RecepcionMedicamento> buscarMedicamentos(
      FiltroRecepcionMedicamento filtro) {
    authorize();
    Stream<RecepcionMedicamento> stream = medicamentos.listarMedicamentos().stream();
    if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
      String term = filtro.busqueda().toLowerCase();
      stream =
          stream.filter(
              r ->
                  r.codigoLote().toLowerCase().contains(term)
                      || r.nombreMedicamentoHistorico().toLowerCase().contains(term)
                      || r.id().toString().contains(term));
    }
    if (filtro.presentacion() != null && !filtro.presentacion().isBlank())
      stream = stream.filter(r -> r.presentacion().equalsIgnoreCase(filtro.presentacion()));
    if (filtro.estado() != null) stream = stream.filter(r -> r.estado() == filtro.estado());
    if (filtro.desde() != null)
      stream = stream.filter(r -> !r.fechaIngreso().isBefore(filtro.desde()));
    if (filtro.hasta() != null)
      stream = stream.filter(r -> !r.fechaIngreso().isAfter(filtro.hasta()));
    return pagina(
        stream.sorted(Comparator.comparing(RecepcionMedicamento::fechaIngreso).reversed()).toList(),
        filtro.page(),
        filtro.size());
  }

  private <T> PaginaResultado<T> pagina(List<T> resultados, int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw new BusinessRuleViolationException(
          "PAGINACION_INVALIDA", "La página debe ser positiva y el tamaño estar entre 1 y 100");
    int from = Math.min(page * size, resultados.size());
    int to = Math.min(from + size, resultados.size());
    return new PaginaResultado<>(
        resultados.subList(from, to),
        page,
        size,
        resultados.size(),
        (int) Math.ceil((double) resultados.size() / size));
  }

  private void authorize() {
    RecepcionAuthorization.requireAdministrador(actor);
  }

  private void auditar(UUID id, Object anterior, Object actual) {
    auditoria.guardar(
        new RegistroAuditoriaRecepcion(
            UUID.randomUUID(),
            id,
            actor.currentActor().id(),
            clock.instant(),
            snapshot(anterior),
            snapshot(actual)));
  }

  private String snapshot(Object value) {
    return "{\"snapshot\":\""
        + value.toString().replace("\\", "\\\\").replace("\"", "\\\"")
        + "\"}";
  }

  public record RecepcionAlimentoConfirmadaV1(RecepcionAlimento recepcion) {}

  public record RecepcionAlimentoActualizadaV1(RecepcionAlimento recepcion) {}

  public record RecepcionMedicamentoConfirmadaV1(RecepcionMedicamento recepcion) {}

  public record RecepcionMedicamentoActualizadaV1(RecepcionMedicamento recepcion) {}
}
