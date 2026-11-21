package com.avicontrol.application.sanitario.diagnostico;

import com.avicontrol.application.sanitario.diagnostico.result.DiagnosticoGalponResult;
import com.avicontrol.domain.exception.sanitario.aislamiento.GalponNoEncontradoException;
import com.avicontrol.domain.exception.sanitario.aislamiento.LoteActivoNoEncontradoException;
import com.avicontrol.domain.exception.sanitario.diagnostico.DatosDiagnosticoInvalidosException;
import com.avicontrol.domain.exception.sanitario.diagnostico.DiagnosticoActivoExistenteException;
import com.avicontrol.domain.exception.sanitario.diagnostico.GalponNoAisladoException;
import com.avicontrol.domain.exception.sanitario.diagnostico.MedicacionRequeridaException;
import com.avicontrol.domain.exception.sanitario.enfermedad.EnfermedadNoEncontradaException;
import com.avicontrol.domain.exception.sanitario.medicacion.MedicacionNoEncontradaException;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.MedicacionQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarDiagnosticoUseCase {
  private final DiagnosticoGalponRepositoryPort diagnosticos;
  private final EnfermedadQueryPort enfermedades;
  private final MedicacionQueryPort medicaciones;
  private final GalponLoteAislamientoQueryPort galpones;
  private final Clock clock;

  public RegistrarDiagnosticoUseCase(
      DiagnosticoGalponRepositoryPort diagnosticos,
      EnfermedadQueryPort enfermedades,
      MedicacionQueryPort medicaciones,
      GalponLoteAislamientoQueryPort galpones,
      Clock clock) {
    this.diagnosticos = diagnosticos;
    this.enfermedades = enfermedades;
    this.medicaciones = medicaciones;
    this.galpones = galpones;
    this.clock = clock;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public DiagnosticoGalponResult registrar(RegistrarDiagnosticoCommand command) {
    if (command == null || command.galponId() == null || command.enfermedadId() == null)
      throw new DatosDiagnosticoInvalidosException("Galpón y enfermedad obligatorios");
    Enfermedad enfermedad =
        enfermedades
            .buscarEnfermedad(command.enfermedadId())
            .orElseThrow(EnfermedadNoEncontradaException::new);
    Medicacion medicacion = null;
    if (!enfermedad.requiereSacrificioSanitario()) {
      if (command.medicacionId() == null) throw new MedicacionRequeridaException();
      medicacion =
          medicaciones
              .buscarMedicacion(command.medicacionId())
              .orElseThrow(MedicacionNoEncontradaException::new);
    } else if (command.medicacionId() != null) {
      throw new DatosDiagnosticoInvalidosException(
          "No se admite medicación para sacrificio sanitario");
    }
    GalponLoteAislamiento contexto =
        galpones.consultar(command.galponId()).orElseThrow(GalponNoEncontradoException::new);
    if (!"AISLAMIENTO".equals(contexto.estado())) throw new GalponNoAisladoException();
    if (contexto.loteId() == null) throw new LoteActivoNoEncontradoException();
    if (enfermedad.requiereSacrificioSanitario()
        ? diagnosticos.existeActivo(command.galponId())
        : diagnosticos.existeSanitarioActivo(command.galponId())
            || diagnosticos.existeActivoMismaEnfermedad(command.galponId(), command.enfermedadId()))
      throw new DiagnosticoActivoExistenteException();
    DiagnosticoGalpon diagnostico =
        DiagnosticoGalpon.crear(
            command.galponId(),
            contexto.loteId(),
            enfermedad,
            medicacion,
            command.observaciones(),
            LocalDate.now(clock));
    diagnosticos.crear(diagnostico);
    return DiagnosticoGalponResult.de(diagnostico);
  }
}
