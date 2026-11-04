package com.avicontrol.infrastructure.adapter.in.rest.recepcion;

import com.avicontrol.application.recepcion.*;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recepciones/medicamentos")
public class RecepcionMedicamentoController {
  private final GestionarRecepcionesUseCase useCase;

  public RecepcionMedicamentoController(GestionarRecepcionesUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  public ResponseEntity<RecepcionMedicamento> registrar(@Valid @RequestBody Request r) {
    RecepcionMedicamento x = useCase.registrarMedicamento(r.command());
    return ResponseEntity.created(URI.create("/api/recepciones/medicamentos/" + x.id())).body(x);
  }

  @PutMapping("/{recepcionId}")
  public RecepcionMedicamento editar(
      @PathVariable UUID recepcionId, @Valid @RequestBody Request r) {
    return useCase.editarMedicamento(recepcionId, r.command());
  }

  @GetMapping
  public PaginaResultado<RecepcionMedicamento> listar(
      @RequestParam(required = false) String busqueda,
      @RequestParam(required = false) String presentacion,
      @RequestParam(required = false) EstadoRecepcion estado,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return useCase.buscarMedicamentos(
        new FiltroRecepcionMedicamento(busqueda, presentacion, estado, desde, hasta, page, size));
  }

  @GetMapping("/{recepcionId}")
  public RecepcionMedicamento detalle(@PathVariable UUID recepcionId) {
    return useCase.detalleMedicamento(recepcionId);
  }

  public record Request(
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
    RecepcionMedicamentoCommand command() {
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
