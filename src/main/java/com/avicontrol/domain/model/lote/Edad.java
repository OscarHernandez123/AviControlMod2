package com.avicontrol.domain.model.lote;

public record Edad(long diasTotales, long semanasCompletas, long diasRestantes, String texto) {
  public static Edad deDias(long dias) {
    if (dias < 1) throw new IllegalArgumentException("La edad debe ser positiva");
    long semanas = dias / 7;
    long restantes = dias % 7;
    String texto =
        semanas == 0
            ? dias + (dias == 1 ? " día" : " días")
            : semanas
                + (semanas == 1 ? " semana" : " semanas")
                + " y "
                + restantes
                + (restantes == 1 ? " día" : " días")
                + " ("
                + dias
                + (dias == 1 ? " día)" : " días)");
    return new Edad(dias, semanas, restantes, texto);
  }
}
