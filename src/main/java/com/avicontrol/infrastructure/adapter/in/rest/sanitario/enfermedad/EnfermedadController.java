package com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad;

import com.avicontrol.application.sanitario.enfermedad.EditarEnfermedadUseCase;
import com.avicontrol.application.sanitario.enfermedad.ListarEnfermedadesUseCase;
import com.avicontrol.application.sanitario.enfermedad.RegistrarEnfermedadUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad.dto.EditarEnfermedadRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad.dto.EnfermedadResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad.dto.RegistrarEnfermedadRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad.mapper.EnfermedadRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enfermedades")
@SecurityRequirement(name = "basicAuth")
public class EnfermedadController {
  private final RegistrarEnfermedadUseCase registro;
  private final EditarEnfermedadUseCase edicion;
  private final ListarEnfermedadesUseCase listado;
  private final EnfermedadRestMapper mapper;

  public EnfermedadController(
      RegistrarEnfermedadUseCase registro,
      EditarEnfermedadUseCase edicion,
      ListarEnfermedadesUseCase listado,
      EnfermedadRestMapper mapper) {
    this.registro = registro;
    this.edicion = edicion;
    this.listado = listado;
    this.mapper = mapper;
  }

  @GetMapping
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(summary = "Listar enfermedades registradas")
  public List<EnfermedadResponse> listar() {
    return listado.listar().stream().map(mapper::response).toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(summary = "Registrar enfermedad")
  public ResponseEntity<EnfermedadResponse> registrar(
      @Valid @RequestBody RegistrarEnfermedadRequest request) {
    EnfermedadResponse response = mapper.response(registro.registrar(mapper.command(request)));
    return ResponseEntity.created(URI.create("/api/enfermedades/" + response.id())).body(response);
  }

  @PutMapping("/{enfermedadId}")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(summary = "Editar enfermedad")
  public EnfermedadResponse editar(
      @PathVariable UUID enfermedadId, @Valid @RequestBody EditarEnfermedadRequest request) {
    return mapper.response(edicion.editar(mapper.command(enfermedadId, request)));
  }
}
