package com.avicontrol.infrastructure.adapter.in.rest.nutricion;

import com.avicontrol.application.nutricion.RegistrarAlimentoComercialUseCase;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nutricion/alimentos")
public class AlimentoComercialController {
  private final RegistrarAlimentoComercialUseCase useCase;

  public AlimentoComercialController(RegistrarAlimentoComercialUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('NUTRICIONISTA')")
  public AlimentoCompatibleQueryPort.Alimento registrar(
      @RequestBody RegistrarAlimentoComercialUseCase.Datos datos) {
    return useCase.registrar(datos);
  }
}
