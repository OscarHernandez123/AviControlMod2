package com.avicontrol.domain.model.sanitario.diagnostico;

import com.avicontrol.domain.exception.sanitario.diagnostico.DatosDiagnosticoInvalidosException;
import com.avicontrol.domain.exception.sanitario.diagnostico.MedicacionNoCompatibleException;
import com.avicontrol.domain.exception.sanitario.diagnostico.MedicacionRequeridaException;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import java.time.LocalDate;
import java.util.UUID;

public record DiagnosticoGalpon(
    UUID id,
    UUID galponId,
    UUID loteId,
    UUID enfermedadId,
    String enfermedadNombre,
    boolean requiereSacrificioSanitario,
    UUID medicacionId,
    UUID medicamentoId,
    String dosis,
    Integer duracionDias,
    String descripcionMedicacion,
    String observaciones,
    LocalDate fechaDiagnostico,
    LocalDate fechaReintegro,
    EstadoDiagnostico estado,
    LocalDate fechaResolucion,
    long version) {

  public DiagnosticoGalpon {
    if (id == null
        || galponId == null
        || loteId == null
        || enfermedadId == null
        || fechaDiagnostico == null
        || estado == null
        || version < 0)
      throw new DatosDiagnosticoInvalidosException("Identificadores, fecha o estado inválidos");
    enfermedadNombre = texto(enfermedadNombre, "enfermedadNombre");
    observaciones = texto(observaciones, "observaciones");
    if (requiereSacrificioSanitario) {
      if (medicacionId != null
          || medicamentoId != null
          || dosis != null
          || duracionDias != null
          || descripcionMedicacion != null
          || fechaReintegro != null)
        throw new DatosDiagnosticoInvalidosException(
            "El sacrificio sanitario no lleva medicación ni reintegro");
      if (estado != EstadoDiagnostico.ACTIVO)
        throw new DatosDiagnosticoInvalidosException("Un diagnóstico sanitario no se reintegra");
    } else {
      if (medicacionId == null
          || medicamentoId == null
          || duracionDias == null
          || duracionDias <= 0
          || fechaReintegro == null) throw new MedicacionRequeridaException();
      dosis = texto(dosis, "dosis");
      descripcionMedicacion = texto(descripcionMedicacion, "descripcionMedicacion");
      if (!fechaReintegro.equals(fechaDiagnostico.plusDays(duracionDias)))
        throw new DatosDiagnosticoInvalidosException("Fecha de reintegro inconsistente");
    }
    if ((estado == EstadoDiagnostico.ACTIVO) != (fechaResolucion == null))
      throw new DatosDiagnosticoInvalidosException("Resolución inconsistente");
  }

  public static DiagnosticoGalpon crear(
      UUID galponId,
      UUID loteId,
      Enfermedad enfermedad,
      Medicacion medicacion,
      String observaciones,
      LocalDate fecha) {
    if (enfermedad == null) throw new DatosDiagnosticoInvalidosException("Enfermedad obligatoria");
    if (enfermedad.requiereSacrificioSanitario()) {
      if (medicacion != null)
        throw new DatosDiagnosticoInvalidosException(
            "No se admite medicación para sacrificio sanitario");
      return new DiagnosticoGalpon(
          UUID.randomUUID(),
          galponId,
          loteId,
          enfermedad.id(),
          enfermedad.nombre(),
          true,
          null,
          null,
          null,
          null,
          null,
          observaciones,
          fecha,
          null,
          EstadoDiagnostico.ACTIVO,
          null,
          0);
    }
    if (medicacion == null) throw new MedicacionRequeridaException();
    if (!medicacion.enfermedadId().equals(enfermedad.id()))
      throw new MedicacionNoCompatibleException();
    return new DiagnosticoGalpon(
        UUID.randomUUID(),
        galponId,
        loteId,
        enfermedad.id(),
        enfermedad.nombre(),
        false,
        medicacion.id(),
        medicacion.medicamentoId(),
        medicacion.dosis(),
        medicacion.duracionDias(),
        medicacion.descripcion(),
        observaciones,
        fecha,
        fecha.plusDays(medicacion.duracionDias()),
        EstadoDiagnostico.ACTIVO,
        null,
        0);
  }

  public DiagnosticoGalpon resolver(EstadoDiagnostico nuevoEstado, LocalDate fecha) {
    if (estado != EstadoDiagnostico.ACTIVO
        || requiereSacrificioSanitario
        || nuevoEstado == EstadoDiagnostico.ACTIVO)
      throw new DatosDiagnosticoInvalidosException("Diagnóstico no reintegrable");
    return new DiagnosticoGalpon(
        id,
        galponId,
        loteId,
        enfermedadId,
        enfermedadNombre,
        false,
        medicacionId,
        medicamentoId,
        dosis,
        duracionDias,
        descripcionMedicacion,
        observaciones,
        fechaDiagnostico,
        fechaReintegro,
        nuevoEstado,
        fecha,
        version + 1);
  }

  private static String texto(String value, String field) {
    if (value == null || value.isBlank())
      throw new DatosDiagnosticoInvalidosException(field + " es obligatorio");
    return value.strip();
  }
}
