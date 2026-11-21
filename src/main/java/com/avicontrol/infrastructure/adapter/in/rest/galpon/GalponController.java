package com.avicontrol.infrastructure.adapter.in.rest.galpon;

import com.avicontrol.application.galpon.ConsultarGalponUseCase;
import com.avicontrol.application.galpon.ConsultarListadoGalponesUseCase;
import com.avicontrol.application.galpon.ConsultarResumenGalponesUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.GalponDetalleResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.ListadoGalponesResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.ResumenGalponesResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.mapper.GalponRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/galpones")
@SecurityRequirement(name = "basicAuth")
public class GalponController {
  private final ConsultarGalponUseCase detalle;
  private final ConsultarListadoGalponesUseCase listado;
  private final ConsultarResumenGalponesUseCase resumen;
  private final GalponRestMapper mapper;

  public GalponController(
      ConsultarGalponUseCase detalle,
      ConsultarListadoGalponesUseCase listado,
      ConsultarResumenGalponesUseCase resumen,
      GalponRestMapper mapper) {
    this.detalle = detalle;
    this.listado = listado;
    this.resumen = resumen;
    this.mapper = mapper;
  }

  @GetMapping("/{galponId}")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR', 'VETERINARIO', 'NUTRICIONISTA')")
  @Operation(
      summary = "Consultar detalle vigente de un galpón",
      description =
          "Roles: administrador, trabajador, veterinario y nutricionista. Estados: DISPONIBLE, VACIADO_SANITARIO, PRODUCTIVO, EN_COSECHA, MANTENIMIENTO y AISLAMIENTO.")
  public GalponDetalleResponse detalle(@PathVariable UUID galponId) {
    return mapper.response(detalle.consultar(galponId));
  }

  @GetMapping("/resumen")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Consultar conteo de galpones por estado",
      description = "Solo administrador. Los estados desconocidos se cuentan como inconsistencias.")
  public ResumenGalponesResponse resumen() {
    return mapper.response(resumen.consultar());
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TRABAJADOR', 'VETERINARIO', 'NUTRICIONISTA')")
  @Operation(
      summary = "Listar galpones con paginación y orden estable",
      description =
          "Roles: administrador, trabajador, veterinario y nutricionista. page inicia en 0; size admite 1 a 100; sort: nombre, estado o id con asc o desc. id desempata el orden.")
  public ListadoGalponesResponse listado(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "nombre,asc") String sort) {
    return mapper.response(listado.consultar(page, size, sort));
  }
}
