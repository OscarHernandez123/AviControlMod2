package com.avicontrol.infrastructure.adapter.in.rest.modulo1;

import com.avicontrol.application.modulo1.eventos.SolicitarEventoModulo1UseCase;
import com.avicontrol.application.modulo1.eventos.SolicitarEventoModulo1UseCase.Tipo;
import com.avicontrol.domain.port.out.modulo1.EventosModulo1Port;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integracion/modulo1/eventos")
@Profile("modulo1-rest & !local & !test")
public class EventosModulo1Controller {
  private final SolicitarEventoModulo1UseCase caso;
  private final EventosModulo1Port eventos;

  public EventosModulo1Controller(SolicitarEventoModulo1UseCase caso, EventosModulo1Port eventos) {
    this.caso = caso;
    this.eventos = eventos;
  }

  public record Solicitud(
      UUID alertaId,
      UUID galponId,
      UUID loteId,
      OffsetDateTime fechaHoraEvento,
      OffsetDateTime fechaHora,
      Object cantidadMuertos,
      String accion,
      String tipoEnfermedad,
      String descripcion,
      String gravedad) {}

  @PostMapping("/mortalidad")
  @PreAuthorize("hasAnyRole('TRABAJADOR','ADMINISTRADOR')")
  public ResponseEntity<EventosModulo1Port.Entrega> mortalidad(@RequestBody Solicitud s) {
    return solicitar(Tipo.MORTALIDAD, s);
  }

  @PostMapping("/alerta-sanitaria")
  @PreAuthorize("hasAnyRole('VETERINARIO','ADMINISTRADOR')")
  public ResponseEntity<EventosModulo1Port.Entrega> sanitaria(@RequestBody Solicitud s) {
    return solicitar(Tipo.SANITARIA, s);
  }

  @PostMapping("/vaciado-sanitario")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ResponseEntity<EventosModulo1Port.Entrega> vaciado(@RequestBody Solicitud s) {
    return solicitar(Tipo.VACIADO, s);
  }

  @GetMapping("/{alertaId}")
  @PreAuthorize("hasAnyRole('TRABAJADOR','VETERINARIO','ADMINISTRADOR')")
  public EventosModulo1Port.Entrega estado(@PathVariable UUID alertaId) {
    return eventos.consultar(alertaId);
  }

  private ResponseEntity<EventosModulo1Port.Entrega> solicitar(Tipo tipo, Solicitud s) {
    if (s == null) throw new IllegalArgumentException("Solicitud obligatoria");
    Integer cantidad = null;
    if (tipo == Tipo.MORTALIDAD) {
      if (!(s.cantidadMuertos() instanceof Integer))
        throw new IllegalArgumentException(
            "cantidadMuertos debe ser entero JSON, sin decimales ni texto");
      cantidad = (Integer) s.cantidadMuertos();
    }
    var entrega =
        caso.solicitar(
            tipo,
            new SolicitarEventoModulo1UseCase.Solicitud(
                s.alertaId(),
                s.galponId(),
                s.loteId(),
                tipo == Tipo.SANITARIA ? s.fechaHora() : s.fechaHoraEvento(),
                cantidad,
                s.accion(),
                s.tipoEnfermedad(),
                s.descripcion(),
                s.gravedad()));
    return ResponseEntity.accepted().body(entrega);
  }
}
