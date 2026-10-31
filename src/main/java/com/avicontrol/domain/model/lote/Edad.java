package com.avicontrol.domain.model.lote;

public record Edad(int diasTotales, int semanasCompletas, int diasRestantes, String texto) {

  public Edad {
    if (diasTotales <= 0 || semanasCompletas < 0 || diasRestantes < 0 || diasRestantes > 6) {
      throw new IllegalArgumentException("La edad no es válida");
    }
  }

  public static Edad fromDiasTotales(int diasTotales) {
    int semanas = diasTotales / 7;
    int dias = diasTotales % 7;
    String texto;
    if (semanas == 0) {
      texto = formatoDias(diasTotales);
    } else if (dias == 0) {
      texto = formatoSemanas(semanas) + " (" + formatoDias(diasTotales) + ")";
    } else {
      texto =
          formatoSemanas(semanas)
              + " y "
              + formatoDias(dias)
              + " ("
              + formatoDias(diasTotales)
              + ")";
    }
    return new Edad(diasTotales, semanas, dias, texto);
  }

  private static String formatoSemanas(int semanas) {
    return semanas + (semanas == 1 ? " semana" : " semanas");
  }

  private static String formatoDias(int dias) {
    return dias + (dias == 1 ? " día" : " días");
  }
}
