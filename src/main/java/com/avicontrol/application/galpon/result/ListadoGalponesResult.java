package com.avicontrol.application.galpon.result;

import java.util.List;

public record ListadoGalponesResult(
    List<GalponListadoResult> elementos,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas,
    String mensaje) {}
