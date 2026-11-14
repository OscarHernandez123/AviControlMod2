package com.avicontrol.infrastructure.adapter.in.rest.medicacion;

import com.avicontrol.application.sanitario.medicacion.EditarMedicacionUseCase;
import com.avicontrol.application.sanitario.medicacion.RegistrarMedicacionUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.EditarMedicacionRequest;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.MedicacionResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.RegistrarMedicacionRequest;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.mapper.MedicacionRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medicaciones")
@SecurityRequirement(name = "basicAuth")
public class MedicacionController {
  private final RegistrarMedicacionUseCase registro;
  private final EditarMedicacionUseCase edicion;
  private final MedicacionRestMapper mapper;

  public MedicacionController(
      RegistrarMedicacionUseCase registro,
      EditarMedicacionUseCase edicion,
      MedicacionRestMapper mapper) {
    this.registro = registro;
    this.edicion = edicion;
    this.mapper = mapper;
  }

  @PostMapping
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(
      summary = "Registrar medicación",
      description = "Define un tratamiento sin descontar inventario")
  public ResponseEntity<MedicacionResponse> registrar(
      @Valid @RequestBody RegistrarMedicacionRequest request) {
    MedicacionResponse response = mapper.response(registro.registrar(mapper.command(request)));
    return ResponseEntity.created(URI.create("/api/medicaciones/" + response.id())).body(response);
  }

  @PutMapping("/{medicacionId}")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(
      summary = "Editar medicación",
      description = "Actualiza el tratamiento para diagnósticos futuros")
  public MedicacionResponse editar(
      @PathVariable UUID medicacionId, @Valid @RequestBody EditarMedicacionRequest request) {
    return mapper.response(edicion.editar(mapper.command(medicacionId, request)));
  }
}
