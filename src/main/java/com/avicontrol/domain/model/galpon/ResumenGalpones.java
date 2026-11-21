package com.avicontrol.domain.model.galpon;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record ResumenGalpones(
    int totalRegistrado,
    int totalValido,
    int registrosInconsistentes,
    Map<EstadoGalpon, Integer> porEstado) {
  public static ResumenGalpones contar(List<String> estados) {
    EnumMap<EstadoGalpon, Integer> conteos = new EnumMap<>(EstadoGalpon.class);
    for (EstadoGalpon estado : EstadoGalpon.values()) conteos.put(estado, 0);
    int inconsistentes = 0;
    for (String valor : estados) {
      var estado = EstadoGalpon.desde(valor);
      if (estado.isPresent()) conteos.merge(estado.get(), 1, Integer::sum);
      else inconsistentes++;
    }
    return new ResumenGalpones(
        estados.size(), estados.size() - inconsistentes, inconsistentes, Map.copyOf(conteos));
  }
}
