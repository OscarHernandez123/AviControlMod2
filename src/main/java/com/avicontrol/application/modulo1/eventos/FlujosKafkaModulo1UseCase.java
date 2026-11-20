package com.avicontrol.application.modulo1.eventos;

import com.avicontrol.application.mortalidad.RegistrarMortalidadCommand;
import com.avicontrol.application.sanitario.aislamiento.ValidarAislamientoCommand;
import com.avicontrol.domain.model.sanitario.aislamiento.*;
import com.avicontrol.domain.port.out.modulo1.*;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

public class FlujosKafkaModulo1UseCase {
  public record Resultado(
      UUID id,
      UUID galponId,
      UUID loteId,
      String estado,
      String estadoGalponResultante,
      EventosModulo1Port.Entrega integracion,
      String tipo) {}

  private final OperacionesModulo1Port operaciones;
  private final EventosModulo1Port entregas;
  private final SolicitarEventoModulo1UseCase eventos;
  private final LoteQueryPort lotes;
  private final GalponQueryPort galpones;
  private final SolicitudAislamientoRepositoryPort solicitudes;
  private final Clock clock;

  public FlujosKafkaModulo1UseCase(
      OperacionesModulo1Port operaciones,
      EventosModulo1Port entregas,
      SolicitarEventoModulo1UseCase eventos,
      LoteQueryPort lotes,
      GalponQueryPort galpones,
      SolicitudAislamientoRepositoryPort solicitudes,
      Clock clock) {
    this.operaciones = operaciones;
    this.entregas = entregas;
    this.eventos = eventos;
    this.lotes = lotes;
    this.galpones = galpones;
    this.solicitudes = solicitudes;
    this.clock = clock;
  }

  @Transactional
  public Resultado mortalidad(RegistrarMortalidadCommand c) {
    if (c == null
        || c.idempotencyKey() == null
        || c.galponId() == null
        || c.fechaHoraEvento() == null
        || c.causa() == null
        || c.cantidad() <= 0
        || c.responsable() == null
        || c.responsable().isBlank())
      throw new IllegalArgumentException(
          "Datos de mortalidad incompletos; Idempotency-Key obligatorio");
    var datos = new LinkedHashMap<String, Object>();
    datos.put("galponId", c.galponId().toString());
    datos.put("fechaHoraEvento", c.fechaHoraEvento().toString());
    datos.put("cantidad", c.cantidad());
    datos.put("causa", c.causa().name());
    datos.put("observaciones", c.observaciones() == null ? "" : c.observaciones());
    datos.put("responsable", c.responsable());
    operaciones.bloquear(c.idempotencyKey());
    var previo = operaciones.buscar(c.idempotencyKey());
    if (previo.isPresent()) {
      validarRepetido(previo.get(), "MORTALIDAD", datos);
      return resultado(previo.get());
    }
    var lote =
        lotes
            .consultarActual(c.galponId())
            .orElseThrow(() -> new NoSuchElementException("Lote activo no encontrado"));
    eventos.solicitar(
        SolicitarEventoModulo1UseCase.Tipo.MORTALIDAD,
        new SolicitarEventoModulo1UseCase.Solicitud(
            c.idempotencyKey(),
            c.galponId(),
            lote.id(),
            c.fechaHoraEvento().atZone(clock.getZone()).toOffsetDateTime(),
            c.cantidad(),
            null,
            null,
            null,
            null));
    var op =
        new OperacionesModulo1Port.Operacion(
            c.idempotencyKey(), "MORTALIDAD", c.galponId(), lote.id(), datos);
    operaciones.guardar(op);
    return resultado(op);
  }

  @Transactional
  public Resultado aislamiento(ValidarAislamientoCommand c) {
    if (c == null || c.solicitudId() == null || c.decision() == null)
      throw new IllegalArgumentException("Solicitud y decisión obligatorias");
    var datos =
        Map.<String, Object>of(
            "decision",
            c.decision().name(),
            "observaciones",
            c.observaciones(),
            "veterinario",
            c.veterinario());
    operaciones.bloquear(c.solicitudId());
    var previo = operaciones.buscar(c.solicitudId());
    if (previo.isPresent()) {
      validarRepetido(previo.get(), "AISLAMIENTO", datos);
      return resultado(previo.get());
    }
    var s =
        solicitudes
            .buscar(c.solicitudId())
            .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));
    if (s.estado() == EstadoSolicitudAislamiento.RECHAZADA
        && c.decision() == DecisionAislamiento.RECHAZAR
        && Objects.equals(s.observaciones(), c.observaciones().strip())
        && Objects.equals(s.veterinario(), c.veterinario()))
      return new Resultado(
          s.id(),
          s.galponId(),
          s.loteId(),
          "RECHAZADA",
          galpones.estadoVigente(s.galponId()).orElse(null),
          null,
          "AISLAMIENTO");
    var resuelta = s.decidir(c.decision(), c.observaciones(), c.veterinario(), clock.instant());
    if (c.decision() == DecisionAislamiento.RECHAZAR) {
      if (!solicitudes.resolver(s, resuelta))
        throw new IllegalStateException("La solicitud ya fue resuelta");
      return new Resultado(
          s.id(),
          s.galponId(),
          s.loteId(),
          "RECHAZADA",
          galpones.estadoVigente(s.galponId()).orElse(null),
          null,
          "AISLAMIENTO");
    }
    eventos.solicitar(
        SolicitarEventoModulo1UseCase.Tipo.SANITARIA,
        new SolicitarEventoModulo1UseCase.Solicitud(
            s.id(),
            s.galponId(),
            s.loteId(),
            resuelta.fechaDecision().atZone(clock.getZone()).toOffsetDateTime(),
            null,
            "AISLAMIENTO",
            s.motivo(),
            s.signosClinicos(),
            null));
    if (!solicitudes.resolver(s, resuelta))
      throw new IllegalStateException("La solicitud ya fue resuelta");
    var op =
        new OperacionesModulo1Port.Operacion(
            s.id(), "AISLAMIENTO", s.galponId(), s.loteId(), datos);
    operaciones.guardar(op);
    return resultado(op);
  }

  @Transactional
  public boolean solicitarCambio(UUID id, String tipo, UUID galpon, UUID lote) {
    operaciones.bloquear(id);
    if (operaciones.buscar(id).isPresent()) return false;
    boolean reanudar = "REANUDACION".equals(tipo);
    eventos.solicitar(
        reanudar
            ? SolicitarEventoModulo1UseCase.Tipo.SANITARIA
            : SolicitarEventoModulo1UseCase.Tipo.VACIADO,
        new SolicitarEventoModulo1UseCase.Solicitud(
            id,
            galpon,
            lote,
            OffsetDateTime.now(clock),
            null,
            reanudar ? "REANUDACION" : null,
            null,
            null,
            null));
    operaciones.guardar(new OperacionesModulo1Port.Operacion(id, tipo, galpon, lote, Map.of()));
    return true;
  }

  public java.util.List<Resultado> listar() {
    return operaciones.listar().stream().map(this::resultado).toList();
  }

  public boolean existe(UUID id) {
    return operaciones.buscar(id).isPresent();
  }

  public Resultado consultar(UUID id) {
    return resultado(
        operaciones
            .buscar(id)
            .orElseThrow(() -> new NoSuchElementException("Operación no encontrada")));
  }

  private Resultado resultado(OperacionesModulo1Port.Operacion o) {
    return new Resultado(
        o.id(),
        o.galponId(),
        o.loteId(),
        "AISLAMIENTO".equals(o.tipo()) ? "ACEPTADA" : "SOLICITADA",
        null,
        entregas.consultar(o.id()),
        o.tipo());
  }

  private void validarRepetido(
      OperacionesModulo1Port.Operacion o, String tipo, Map<String, Object> datos) {
    if (!o.tipo().equals(tipo) || !o.datos().equals(datos))
      throw new IllegalStateException("Clave de operación reutilizada con datos distintos");
  }
}
