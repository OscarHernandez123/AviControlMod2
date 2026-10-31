package com.avicontrol.infrastructure.adapter.in.rest.galpon;

import com.avicontrol.application.galpon.ConsultarEdadPorGalponUseCase;
import com.avicontrol.application.galpon.ConsultarGalponUseCase;
import com.avicontrol.application.galpon.ConsultarListadoGalponesUseCase;
import com.avicontrol.application.galpon.ConsultarResumenGalponesUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.EdadLoteResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.GalponDetalleResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.ListadoGalponesResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.ResumenGalponesResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.mapper.GalponRestMapper;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/galpones")
public class GalponController {

  private final ConsultarEdadPorGalponUseCase consultarEdadPorGalponUseCase;
  private final ConsultarGalponUseCase consultarGalponUseCase;
  private final ConsultarResumenGalponesUseCase consultarResumenGalponesUseCase;
  private final ConsultarListadoGalponesUseCase consultarListadoGalponesUseCase;
  private final GalponRestMapper mapper;

  public GalponController(
      ConsultarEdadPorGalponUseCase consultarEdadPorGalponUseCase,
      ConsultarGalponUseCase consultarGalponUseCase,
      ConsultarResumenGalponesUseCase consultarResumenGalponesUseCase,
      ConsultarListadoGalponesUseCase consultarListadoGalponesUseCase,
      GalponRestMapper mapper) {
    this.consultarEdadPorGalponUseCase = consultarEdadPorGalponUseCase;
    this.consultarGalponUseCase = consultarGalponUseCase;
    this.consultarResumenGalponesUseCase = consultarResumenGalponesUseCase;
    this.consultarListadoGalponesUseCase = consultarListadoGalponesUseCase;
    this.mapper = mapper;
  }

  @GetMapping("/{galponId}/edad-lote")
  public ResponseEntity<EdadLoteResponse> consultarEdad(@PathVariable UUID galponId) {
    return ResponseEntity.ok(mapper.toResponse(consultarEdadPorGalponUseCase.execute(galponId)));
  }

  @GetMapping("/resumen")
  public ResponseEntity<ResumenGalponesResponse> consultarResumen() {
    return ResponseEntity.ok(mapper.toResponse(consultarResumenGalponesUseCase.execute()));
  }

  @GetMapping
  public ResponseEntity<ListadoGalponesResponse> listar(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "nombre,asc") String sort) {
    return ResponseEntity.ok(
        mapper.toResponse(consultarListadoGalponesUseCase.execute(page, size, sort)));
  }

  @GetMapping("/{galponId}")
  public ResponseEntity<GalponDetalleResponse> consultarDetalle(@PathVariable UUID galponId) {
    return ResponseEntity.ok(mapper.toResponse(consultarGalponUseCase.execute(galponId)));
  }
}
