package com.avicontrol.application.modulo1.eventos;

import com.avicontrol.domain.port.out.modulo1.EventosModulo1Port;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class SolicitarEventoModulo1UseCase {
  public enum Tipo {
    MORTALIDAD("avicontrol.mortalidad", "MORTALIDAD_REGISTRADA"),
    SANITARIA("avicontrol.alerta-sanitaria", "ALERTA_SANITARIA_EMITIDA"),
    VACIADO("avicontrol.vaciado-sanitario", "VACIADO_SANITARIO_SOLICITADO");
    final String topic;
    final String eventType;

    Tipo(String topic, String eventType) {
      this.topic = topic;
      this.eventType = eventType;
    }
  }

  public record Solicitud(
      UUID alertaId,
      UUID galponId,
      UUID loteId,
      OffsetDateTime fecha,
      Integer cantidadMuertos,
      String accion,
      String tipoEnfermedad,
      String descripcion,
      String gravedad) {}

  private final EventosModulo1Port eventos;
  private final GalponQueryPort galpones;
  private final LoteQueryPort lotes;
  private final Clock clock;

  public SolicitarEventoModulo1UseCase(
      EventosModulo1Port eventos, GalponQueryPort galpones, LoteQueryPort lotes, Clock clock) {
    this.eventos = eventos;
    this.galpones = galpones;
    this.lotes = lotes;
    this.clock = clock;
  }

  @Transactional
  public EventosModulo1Port.Entrega solicitar(Tipo tipo, Solicitud s) {
    if (s.alertaId() == null || s.galponId() == null || s.fecha() == null)
      throw new IllegalArgumentException("alertaId, galponId y fecha con zona son obligatorios");
    // Se requiere galponId también para alertas por lote: es la clave Kafka acordada.
    var fecha = s.fecha().atZoneSameInstant(ZoneId.of("America/Bogota")).toOffsetDateTime();
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("alertaId", s.alertaId().toString());
    payload.put("galponId", s.galponId().toString());
    if (s.loteId() != null) payload.put("loteId", s.loteId().toString());
    payload.put(tipo == Tipo.SANITARIA ? "fechaHora" : "fechaHoraEvento", fecha.toString());
    if (tipo == Tipo.SANITARIA) {
      if (!"AISLAMIENTO".equals(s.accion()) && !"REANUDACION".equals(s.accion()))
        throw new IllegalArgumentException("Acción sanitaria inválida");
      payload.put("accion", s.accion());
      if ("AISLAMIENTO".equals(s.accion())) {
        if (s.tipoEnfermedad() == null
            || s.tipoEnfermedad().isBlank()
            || s.descripcion() == null
            || s.descripcion().isBlank())
          throw new IllegalArgumentException("Aislamiento requiere tipoEnfermedad y descripcion");
        payload.put("tipoEnfermedad", s.tipoEnfermedad());
        payload.put("descripcion", s.descripcion());
        if (s.gravedad() != null) payload.put("gravedad", s.gravedad());
      }
    } else {
      if (s.loteId() == null) throw new IllegalArgumentException("loteId obligatorio");
      if (tipo == Tipo.MORTALIDAD) {
        if (s.cantidadMuertos() == null || s.cantidadMuertos() <= 0)
          throw new IllegalArgumentException("cantidadMuertos debe ser un entero positivo");
        payload.put("cantidadMuertos", s.cantidadMuertos());
      }
    }
    var evento =
        new EventosModulo1Port.Evento(
            s.alertaId(), s.galponId(), tipo.topic, tipo.eventType, fecha, payload);
    var repetido = eventos.repetido(evento);
    if (repetido.isPresent()) return repetido.get();
    if (!fecha
            .toLocalDate()
            .equals(clock.instant().atZone(ZoneId.of("America/Bogota")).toLocalDate())
        || (tipo != Tipo.SANITARIA && fecha.toInstant().isAfter(clock.instant())))
      throw new IllegalArgumentException("Fecha fuera del día de negocio o futura");
    var galpon =
        galpones
            .consultar(s.galponId())
            .orElseThrow(() -> new NoSuchElementException("Galpón no encontrado"));
    String esperado =
        tipo == Tipo.VACIADO
            ? "EN_COSECHA"
            : tipo == Tipo.SANITARIA && "REANUDACION".equals(s.accion())
                ? "AISLAMIENTO"
                : "PRODUCTIVO";
    if (!esperado.equals(galpon.estado()))
      throw new IllegalStateException("El galpón debe estar " + esperado);
    if (s.loteId() != null) {
      var lote =
          lotes
              .consultarActual(s.galponId())
              .orElseThrow(() -> new IllegalStateException("Sin lote activo"));
      if (!s.loteId().equals(lote.id()))
        throw new IllegalStateException("El lote no es el activo del galpón");
      if (tipo == Tipo.MORTALIDAD && s.cantidadMuertos() > lote.poblacionActual())
        throw new IllegalStateException("Cantidad mayor que la población vigente");
    }
    return eventos.encolar(evento);
  }
}
