package com.avicontrol.infrastructure.adapter.in.rest.inventario;

import com.avicontrol.application.inventario.ConsultarInventarioUseCase;
import com.avicontrol.application.inventario.ConsultarResumenInventarioUseCase;
import com.avicontrol.domain.model.inventario.InventarioActual;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {
  private final ConsultarInventarioUseCase inventario;
  private final ConsultarResumenInventarioUseCase resumen;
  private final com.avicontrol.application.inventario.ConsultarCoberturaAlimentoUseCase cobertura;

  public InventarioController(
      ConsultarInventarioUseCase inventario,
      ConsultarResumenInventarioUseCase resumen,
      com.avicontrol.application.inventario.ConsultarCoberturaAlimentoUseCase cobertura) {
    this.inventario = inventario;
    this.resumen = resumen;
    this.cobertura = cobertura;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public InventarioActual consultar() {
    return inventario.consultar();
  }

  @GetMapping("/resumen")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ConsultarResumenInventarioUseCase.Resumen resumen() {
    return resumen.consultar();
  }

  @GetMapping("/alimentos/{alimentoId}/requerimiento")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public com.avicontrol.application.inventario.ConsultarCoberturaAlimentoUseCase.Cobertura
      cobertura(@PathVariable UUID alimentoId) {
    return cobertura.consultar(alimentoId);
  }
}
