package com.avicontrol.application.galpon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.application.galpon.result.EdadLoteResult;
import com.avicontrol.application.galpon.result.GalponDetalleResult;
import com.avicontrol.application.galpon.result.ListadoGalponesResult;
import com.avicontrol.application.galpon.result.ResumenGalponesResult;
import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.galpon.LoteActualInconsistenteException;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.galpon.GalponPage;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.LoteQueryPort;
import com.avicontrol.domain.port.out.galpon.ResumenGalponesFuente;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarGalponesUseCasesTest {

  private final UUID galponId = UUID.randomUUID();
  private final Galpon galpon = new Galpon(galponId, "Galpón 1", 8000, EstadoGalpon.PRODUCTIVO);
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC);
  private final CurrentActorProvider administrador =
      () -> new CurrentActor("admin", java.util.Set.of("ROLE_ADMINISTRADOR"));
  private final CurrentActorProvider trabajador =
      () -> new CurrentActor("worker", java.util.Set.of("ROLE_TRABAJADOR"));

  @Test
  void returnsTheExactMessageWhenTheGalponHasNoCurrentLote() {
    GalponQueryPort galpones = fixedGalpones(List.of(galpon));
    LoteQueryPort lotes = ignored -> Optional.empty();

    EdadLoteResult result =
        new ConsultarEdadPorGalponUseCase(galpones, lotes, administrador, clock).execute(galponId);

    assertThat(result.loteId()).isNull();
    assertThat(result.edad()).isNull();
    assertThat(result.mensaje()).isEqualTo("El galpón no tiene lote alojado actualmente");
  }

  @Test
  void returnsDetailWithADataIncidentInsteadOfANegativeAge() {
    Lote futureLote = lote(LocalDate.of(2026, 11, 1));
    GalponQueryPort galpones = fixedGalpones(List.of(galpon));
    LoteQueryPort lotes = ignored -> Optional.of(futureLote);

    GalponDetalleResult result =
        new ConsultarGalponUseCase(galpones, lotes, trabajador, clock).execute(galponId);

    assertThat(result.loteActual().edad()).isNull();
    assertThat(result.incidencias())
        .singleElement()
        .extracting("codigo")
        .isEqualTo("FECHA_INGRESO_INCONSISTENTE");
  }

  @Test
  void rejectsMultipleCurrentLotesWithoutSelectingOneArbitrarily() {
    GalponQueryPort galpones = fixedGalpones(List.of(galpon));
    LoteQueryPort lotes =
        ignored -> {
          throw new LoteActualInconsistenteException();
        };

    assertThatThrownBy(
            () -> new ConsultarGalponUseCase(galpones, lotes, trabajador, clock).execute(galponId))
        .isInstanceOf(LoteActualInconsistenteException.class);
  }

  @Test
  void keepsAllSixStatesInTheSummaryIncludingZeros() {
    Map<EstadoGalpon, Long> counts = new EnumMap<>(EstadoGalpon.class);
    counts.put(EstadoGalpon.PRODUCTIVO, 2L);
    GalponQueryPort galpones =
        new GalponQueryPort() {
          @Override
          public Optional<Galpon> buscarPorId(UUID id) {
            return Optional.empty();
          }

          @Override
          public GalponPage listar(GalponPageQuery query) {
            return new GalponPage(List.of(), 0);
          }

          @Override
          public ResumenGalponesFuente consultarResumen() {
            return new ResumenGalponesFuente(3, 1, counts);
          }
        };

    ResumenGalponesResult result =
        new ConsultarResumenGalponesUseCase(galpones, administrador).execute();

    assertThat(result.totalValido()).isEqualTo(2);
    assertThat(result.porEstado()).hasSize(6).containsEntry(EstadoGalpon.AISLAMIENTO, 0L);
  }

  @Test
  void listsTheSamePageForAnyAuthorizedReader() {
    Galpon second = new Galpon(UUID.randomUUID(), "Galpón 2", 6000, EstadoGalpon.DISPONIBLE);
    GalponQueryPort galpones = fixedGalpones(List.of(galpon, second));

    ListadoGalponesResult result =
        new ConsultarListadoGalponesUseCase(galpones, trabajador).execute(0, 20, "nombre,asc");

    assertThat(result.content()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(2);
    assertThat(result.first()).isTrue();
    assertThat(result.last()).isTrue();
  }

  private GalponQueryPort fixedGalpones(List<Galpon> values) {
    return new GalponQueryPort() {
      @Override
      public Optional<Galpon> buscarPorId(UUID id) {
        return values.stream().filter(g -> g.id().equals(id)).findFirst();
      }

      @Override
      public GalponPage listar(GalponPageQuery query) {
        return new GalponPage(values, values.size());
      }

      @Override
      public ResumenGalponesFuente consultarResumen() {
        return new ResumenGalponesFuente(0, 0, Map.of());
      }
    };
  }

  private Lote lote(LocalDate fechaIngreso) {
    return new Lote(
        UUID.randomUUID(), "Lote 1", 8000, 7960, fechaIngreso, BigDecimal.TEN, galponId);
  }
}
