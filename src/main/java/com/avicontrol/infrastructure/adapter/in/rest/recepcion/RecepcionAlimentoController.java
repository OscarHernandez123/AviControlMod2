package com.avicontrol.infrastructure.adapter.in.rest.recepcion;

import com.avicontrol.application.recepcion.*;
import com.avicontrol.domain.model.catalogo.TipoAlimento;
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
@RequestMapping("/api/recepciones/alimentos")
public class RecepcionAlimentoController {
  private final GestionarRecepcionesUseCase useCase;

  public RecepcionAlimentoController(GestionarRecepcionesUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  public ResponseEntity<RecepcionAlimento> registrar(@Valid @RequestBody Request r) {
    RecepcionAlimento x = useCase.registrarAlimento(r.command());
    return ResponseEntity.created(URI.create("/api/recepciones/alimentos/" + x.id())).body(x);
  }

  @PutMapping("/{recepcionId}")
  public RecepcionAlimento editar(@PathVariable UUID recepcionId, @Valid @RequestBody Request r) {
    return useCase.editarAlimento(recepcionId, r.command());
  }

  @GetMapping
  public PaginaResultado<RecepcionAlimento> listar(
      @RequestParam(required = false) String busqueda,
      @RequestParam(required = false) EstadoRecepcion estado,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return useCase.buscarAlimentos(
        new FiltroRecepcionAlimento(busqueda, estado, desde, hasta, page, size));
  }

  @GetMapping("/{recepcionId}")
  public RecepcionAlimento detalle(@PathVariable UUID recepcionId) {
    return useCase.detalleAlimento(recepcionId);
  }

  public record Request(
      @NotNull UUID alimentoId,
      @NotNull TipoAlimento tipoAlimento,
      @NotBlank String codigoLote,
      @NotNull LocalDate fechaIngreso,
      @NotNull LocalDate fechaVencimiento,
      @NotNull @DecimalMin("0.0001") BigDecimal cantidadBultos,
      @NotNull @DecimalMin("0.0001") BigDecimal pesoPorBultoKg,
      @NotNull @DecimalMin("0.01") BigDecimal precioNetoPorBulto,
      @NotNull @DecimalMin("0") BigDecimal porcentajeImpuesto) {
    RecepcionAlimentoCommand command() {
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
}
