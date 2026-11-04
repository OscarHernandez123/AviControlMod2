package com.avicontrol.infrastructure.adapter.in.rest.catalogo;

import com.avicontrol.application.catalogo.*;
import com.avicontrol.domain.model.catalogo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {
  private final GestionarCatalogoUseCase useCase;

  public CatalogoController(GestionarCatalogoUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/alimentos")
  public ResponseEntity<Alimento> registrarAlimento(@Valid @RequestBody AlimentoRequest r) {
    Alimento a = useCase.registrarAlimento(r.command());
    return ResponseEntity.created(URI.create("/api/catalogo/alimentos/" + a.id())).body(a);
  }

  @PutMapping("/alimentos/{id}")
  public Alimento editarAlimento(@PathVariable UUID id, @Valid @RequestBody AlimentoRequest r) {
    return useCase.editarAlimento(id, r.command());
  }

  @GetMapping("/alimentos")
  public List<Alimento> listarAlimentos(
      @RequestParam(required = false) String busqueda,
      @RequestParam(required = false) EstadoProductoCatalogo estado) {
    return useCase.listarAlimentos().stream()
        .filter(a -> estado == null || a.estado() == estado)
        .filter(
            a ->
                busqueda == null
                    || busqueda.isBlank()
                    || a.nombreComercial().toLowerCase().contains(busqueda.toLowerCase())
                    || a.marca().toLowerCase().contains(busqueda.toLowerCase()))
        .toList();
  }

  @GetMapping("/alimentos/{id}")
  public Alimento detalleAlimento(@PathVariable UUID id) {
    return useCase.consultarAlimento(id);
  }

  @PostMapping("/medicamentos")
  public ResponseEntity<Medicamento> registrarMedicamento(
      @Valid @RequestBody MedicamentoRequest r) {
    Medicamento m = useCase.registrarMedicamento(r.command());
    return ResponseEntity.created(URI.create("/api/catalogo/medicamentos/" + m.id())).body(m);
  }

  @PutMapping("/medicamentos/{id}")
  public Medicamento editarMedicamento(
      @PathVariable UUID id, @Valid @RequestBody MedicamentoRequest r) {
    return useCase.editarMedicamento(id, r.command());
  }

  @GetMapping("/medicamentos")
  public List<Medicamento> listarMedicamentos(
      @RequestParam(required = false) String busqueda,
      @RequestParam(required = false) EstadoProductoCatalogo estado) {
    return useCase.listarMedicamentos().stream()
        .filter(m -> estado == null || m.estado() == estado)
        .filter(
            m ->
                busqueda == null
                    || busqueda.isBlank()
                    || m.nombreComercial().toLowerCase().contains(busqueda.toLowerCase())
                    || m.presentacionCompatible().toLowerCase().contains(busqueda.toLowerCase()))
        .toList();
  }

  @GetMapping("/medicamentos/{id}")
  public Medicamento detalleMedicamento(@PathVariable UUID id) {
    return useCase.consultarMedicamento(id);
  }

  public record AlimentoRequest(
      @NotBlank String nombreComercial,
      @NotBlank String marca,
      @NotNull TipoAlimento tipo,
      @NotNull @DecimalMin("0.0001") BigDecimal pesoNominalBultoKg,
      @Positive Integer duracionRecomendadaEtapaDias,
      @NotNull EstadoProductoCatalogo estado) {
    AlimentoCommand command() {
      return new AlimentoCommand(
          nombreComercial, marca, tipo, pesoNominalBultoKg, duracionRecomendadaEtapaDias, estado);
    }
  }

  public record MedicamentoRequest(
      @NotBlank String nombreComercial,
      @NotBlank String presentacionCompatible,
      @NotNull UnidadMedidaMedicamento unidadBase,
      @NotNull EstadoProductoCatalogo estado) {
    MedicamentoCommand command() {
      return new MedicamentoCommand(nombreComercial, presentacionCompatible, unidadBase, estado);
    }
  }
}
