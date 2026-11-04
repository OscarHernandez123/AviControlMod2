package com.avicontrol.infrastructure.adapter.in.rest.recepcion;

import com.avicontrol.application.recepcion.GestionarRecepcionesUseCase;
import com.avicontrol.application.recepcion.RecepcionAlimentoCommand;
import com.avicontrol.application.recepcion.RecepcionMedicamentoCommand;
import com.avicontrol.domain.model.catalogo.TipoAlimento;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @deprecated Split controllers expose the public REST API.
 */
@Deprecated
class RecepcionController {
  private final GestionarRecepcionesUseCase useCase;

  public RecepcionController(GestionarRecepcionesUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/alimentos")
  public ResponseEntity<RecepcionAlimento> registrarAlimento(
      @Valid @RequestBody AlimentoRequest r) {
    RecepcionAlimento result = useCase.registrarAlimento(r.toCommand());
    return ResponseEntity.created(URI.create("/api/recepciones/alimentos/" + result.id()))
        .body(result);
  }

  @PutMapping("/alimentos/{id}")
  public RecepcionAlimento editarAlimento(
      @PathVariable UUID id, @Valid @RequestBody AlimentoRequest r) {
    return useCase.editarAlimento(id, r.toCommand());
  }

  @GetMapping("/alimentos")
  public List<RecepcionAlimento> listarAlimentos() {
    return useCase.listarAlimentos();
  }

  @GetMapping("/alimentos/{id}")
  public RecepcionAlimento detalleAlimento(@PathVariable UUID id) {
    return useCase.detalleAlimento(id);
  }

  @PostMapping("/medicamentos")
  public ResponseEntity<RecepcionMedicamento> registrarMedicamento(
      @Valid @RequestBody MedicamentoRequest r) {
    RecepcionMedicamento result = useCase.registrarMedicamento(r.toCommand());
    return ResponseEntity.created(URI.create("/api/recepciones/medicamentos/" + result.id()))
        .body(result);
  }

  @PutMapping("/medicamentos/{id}")
  public RecepcionMedicamento editarMedicamento(
      @PathVariable UUID id, @Valid @RequestBody MedicamentoRequest r) {
    return useCase.editarMedicamento(id, r.toCommand());
  }

  @GetMapping("/medicamentos")
  public List<RecepcionMedicamento> listarMedicamentos() {
    return useCase.listarMedicamentos();
  }

  @GetMapping("/medicamentos/{id}")
  public RecepcionMedicamento detalleMedicamento(@PathVariable UUID id) {
    return useCase.detalleMedicamento(id);
  }

  public record AlimentoRequest(
      @NotNull UUID alimentoId,
      @NotNull TipoAlimento tipoAlimento,
      @NotBlank String codigoLote,
      @NotNull LocalDate fechaIngreso,
      @NotNull LocalDate fechaVencimiento,
      @NotNull @DecimalMin("0.0001") BigDecimal cantidadBultos,
      @NotNull @DecimalMin("0.0001") BigDecimal pesoPorBultoKg,
      @NotNull @DecimalMin("0.01") BigDecimal precioNetoPorBulto,
      @NotNull @DecimalMin("0") BigDecimal porcentajeImpuesto) {
    RecepcionAlimentoCommand toCommand() {
      return new RecepcionAlimentoCommand(
          alimentoId,
          tipoAlimento,
          codigoLote,
          fechaIngreso,
          fechaVencimiento,
          cantidadBultos,
          pesoPorBultoKg,
          precioNetoPorBulto,
          porcentajeImpuesto);
    }
  }

  public record MedicamentoRequest(
      @NotNull UUID medicamentoId,
      @NotBlank String codigoLote,
      @NotBlank String presentacion,
      @NotNull @DecimalMin("0.0001") BigDecimal cantidad,
      @NotNull @DecimalMin("0.0001") BigDecimal contenidoPorPresentacion,
      @NotNull UnidadMedidaMedicamento unidadMedida,
      @NotNull LocalDate fechaIngreso,
      @NotNull LocalDate fechaVencimiento,
      @NotNull @DecimalMin("0.01") BigDecimal precioNetoPorPresentacion,
      @NotNull @DecimalMin("0") BigDecimal porcentajeImpuesto) {
    RecepcionMedicamentoCommand toCommand() {
      return new RecepcionMedicamentoCommand(
          medicamentoId,
          codigoLote,
          presentacion,
          cantidad,
          contenidoPorPresentacion,
          unidadMedida,
          fechaIngreso,
          fechaVencimiento,
          precioNetoPorPresentacion,
          porcentajeImpuesto);
    }
  }
}
