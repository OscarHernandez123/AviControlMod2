package com.avicontrol.application.alerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.alerta.AlertaSanitariaQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarAlertasSanitariasUseCaseTest {
  @Mock AlertaSanitariaQueryPort alertas;
  @Mock GalponQueryPort galpones;
  @Mock LoteQueryPort lotes;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void listaYDetallaAlertasPendientesConDatosDeGalponYLote() {
    ConsultarAlertasSanitariasUseCase useCase =
        new ConsultarAlertasSanitariasUseCase(alertas, galpones, lotes, clock);
    UUID alertaId = UUID.randomUUID();
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();

    var datos =
        new AlertaSanitariaQueryPort.Datos(
            alertaId,
            "ALTA",
            "PENDIENTE_ATENCION_VETERINARIA",
            galponId,
            loteId,
            25,
            new BigDecimal("2.50"),
            "Sospecha respiratoria",
            "Obs",
            "trabajador");
    when(alertas.contar("PENDIENTE_ATENCION_VETERINARIA")).thenReturn(1L);
    when(alertas.listarIds("PENDIENTE_ATENCION_VETERINARIA", 0, 10)).thenReturn(List.of(alertaId));
    when(alertas.buscar(alertaId)).thenReturn(Optional.of(datos));
    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(
                new GalponQueryPort.GalponDatos(galponId, "Galpón 1", 1500, "EN_PRODUCCION")));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 975, LocalDate.of(2026, 11, 10))));

    var pagina = useCase.listar("PENDIENTE_ATENCION_VETERINARIA", 0, 10);

    assertThat(pagina.totalElementos()).isEqualTo(1L);
    assertThat(pagina.elementos()).hasSize(1);
    assertThat(pagina.elementos().get(0).nombreGalpon()).isEqualTo("Galpón 1");
    assertThat(pagina.elementos().get(0).edadDias()).isEqualTo(11L);
  }

  @Test
  void rechazaFiltroInvalidoAlertaInexistenteOModulo1NoDisponible() {
    ConsultarAlertasSanitariasUseCase useCase =
        new ConsultarAlertasSanitariasUseCase(alertas, galpones, lotes, clock);

    assertThatThrownBy(() -> useCase.listar("OTRO_ESTADO", 0, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> useCase.listar("PENDIENTE_ATENCION_VETERINARIA", -1, 10))
        .isInstanceOf(IllegalArgumentException.class);

    UUID id = UUID.randomUUID();
    when(alertas.buscar(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.detalle(id))
        .isInstanceOf(NoSuchElementException.class);

    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    when(alertas.buscar(id))
        .thenReturn(
            Optional.of(
                new AlertaSanitariaQueryPort.Datos(
                    id,
                    "ALTA",
                    "PENDIENTE_ATENCION_VETERINARIA",
                    galponId,
                    loteId,
                    25,
                    new BigDecimal("2.50"),
                    "Causa",
                    null,
                    "trabajador")));
    when(galpones.consultar(galponId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.detalle(id))
        .isInstanceOf(InformacionGalponNoDisponibleException.class);
  }
}
