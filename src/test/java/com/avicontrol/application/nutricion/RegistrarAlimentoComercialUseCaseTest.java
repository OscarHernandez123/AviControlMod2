package com.avicontrol.application.nutricion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.port.out.nutricion.AlimentoComercialRepositoryPort;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrarAlimentoComercialUseCaseTest {
  @Mock AlimentoComercialRepositoryPort repositorio;
  @Mock AlimentoCompatibleQueryPort consulta;
  @InjectMocks RegistrarAlimentoComercialUseCase useCase;

  @Test
  void registraAlimentoNormalizandoNombreYRetornaConsulta() {
    UUID id = UUID.randomUUID();
    var datos =
        new RegistrarAlimentoComercialUseCase.Datos(
            id,
            "  Iniciador Premium  ",
            EtapaCrianza.INICIO,
            new BigDecimal("40"),
            new BigDecimal("21.5"),
            new BigDecimal("3050"),
            14);
    var esperado =
        new AlimentoCompatibleQueryPort.Alimento(
            id,
            "Iniciador Premium",
            EtapaCrianza.INICIO,
            new BigDecimal("40"),
            new BigDecimal("21.5"),
            new BigDecimal("3050"),
            14,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "SIN_STOCK");

    when(consulta.buscar(id)).thenReturn(Optional.of(esperado));

    var resultado = useCase.registrar(datos);

    assertThat(resultado.nombre()).isEqualTo("Iniciador Premium");
    verify(repositorio)
        .registrar(
            id,
            "Iniciador Premium",
            EtapaCrianza.INICIO,
            new BigDecimal("40"),
            new BigDecimal("21.5"),
            new BigDecimal("3050"),
            14);
  }

  @Test
  void rechazaDatosInvalidosOPorcentajeFueraDeRango() {
    var invalido =
        new RegistrarAlimentoComercialUseCase.Datos(
            UUID.randomUUID(),
            "   ",
            EtapaCrianza.ENGORDE,
            new BigDecimal("40"),
            new BigDecimal("105"),
            new BigDecimal("3000"),
            null);

    assertThatThrownBy(() -> useCase.registrar(invalido))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(repositorio);
  }
}
