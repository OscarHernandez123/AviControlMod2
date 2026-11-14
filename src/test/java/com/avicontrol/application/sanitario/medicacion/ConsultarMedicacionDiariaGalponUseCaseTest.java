package com.avicontrol.application.sanitario.medicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.port.out.sanitario.medicacion.DiagnosticoTratamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsultarMedicacionDiariaGalponUseCaseTest {
  private final GalponLoteQueryPort galpones = Mockito.mock(GalponLoteQueryPort.class);
  private final DiagnosticoTratamientoQueryPort diagnosticos =
      Mockito.mock(DiagnosticoTratamientoQueryPort.class);
  private final Clock clock =
      Clock.fixed(
          LocalDate.of(2026, 11, 6).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  private final ConsultarMedicacionDiariaGalponUseCase useCase =
      new ConsultarMedicacionDiariaGalponUseCase(galpones, diagnosticos, clock);

  @Test
  void muestraTratamientoVigenteYDiaSinAplicacion() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    when(galpones.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteQueryPort.GalponLote(
                    galpon,
                    "Galpón 5",
                    10000,
                    "AISLAMIENTO",
                    lote,
                    "LDP-005",
                    8000,
                    LocalDate.of(2026, 11, 1))));
    when(diagnosticos.consultar(lote, LocalDate.of(2026, 11, 6)))
        .thenReturn(
            List.of(
                new DiagnosticoTratamientoQueryPort.Tratamiento(
                    UUID.randomUUID(),
                    LocalDate.of(2026, 11, 5),
                    5,
                    "Coccidiosis",
                    "Amprolio",
                    "Amprolio",
                    "200 g",
                    "Agua",
                    false)));
    var resultado = useCase.consultar(galpon, null);
    assertEquals("CON_MEDICACION_ACTIVA", resultado.estado());
    assertEquals("Día 2 de 5", resultado.tratamientos().getFirst().progreso().etiqueta());
    assertEquals("0", resultado.tratamientos().getFirst().dosisHoy());
  }

  @Test
  void sinPoblacionNoConsultaDiagnosticos() {
    UUID galpon = UUID.randomUUID();
    when(galpones.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteQueryPort.GalponLote(
                    galpon,
                    "Galpón vacío",
                    10000,
                    "PRODUCTIVA",
                    UUID.randomUUID(),
                    "L-1",
                    0,
                    LocalDate.of(2026, 11, 1))));
    assertEquals("SIN_MEDICACION_ACTIVA", useCase.consultar(galpon, null).estado());
    verifyNoInteractions(diagnosticos);
  }

  @Test
  void galponInexistenteDaError() {
    UUID galpon = UUID.randomUUID();
    when(galpones.consultar(galpon)).thenReturn(Optional.empty());
    assertThrows(java.util.NoSuchElementException.class, () -> useCase.consultar(galpon, null));
  }

  @Test
  void listaTratamientosSeparadosYExcluyeUnoFinalizado() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    LocalDate hoy = LocalDate.of(2026, 11, 6);
    when(galpones.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteQueryPort.GalponLote(
                    galpon, "Galpón 1", 1000, "AISLAMIENTO", lote, "L-1", 900, hoy.minusDays(10))));
    when(diagnosticos.consultar(lote, hoy))
        .thenReturn(
            List.of(
                new DiagnosticoTratamientoQueryPort.Tratamiento(
                    UUID.randomUUID(), hoy.minusDays(1), 5, "E1", "M1", "P1", "1 ml", "Agua", true),
                new DiagnosticoTratamientoQueryPort.Tratamiento(
                    UUID.randomUUID(), hoy, 3, "E2", "M2", "P2", "2 ml", "Agua", true),
                new DiagnosticoTratamientoQueryPort.Tratamiento(
                    UUID.randomUUID(),
                    hoy.minusDays(5),
                    5,
                    "E3",
                    "M3",
                    "P3",
                    "3 ml",
                    "Agua",
                    true)));
    var resultado = useCase.consultar(galpon, hoy);
    assertEquals(2, resultado.tratamientos().size());
    assertEquals("M1", resultado.tratamientos().get(0).medicamento());
    assertEquals("M2", resultado.tratamientos().get(1).medicamento());
  }
}
