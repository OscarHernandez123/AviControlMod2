package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.GalponListadoResult;
import com.avicontrol.application.galpon.result.ListadoGalponesResult;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.Set;

public class ConsultarListadoGalponesUseCase {
  private static final Set<String> CAMPOS = Set.of("nombre", "estado", "id");
  private final GalponQueryPort galpones;
  private final com.avicontrol.domain.port.out.modulo1.LoteQueryPort lotes;

  public ConsultarListadoGalponesUseCase(GalponQueryPort galpones) {
    this(galpones, null);
  }

  public ConsultarListadoGalponesUseCase(
      GalponQueryPort galpones, com.avicontrol.domain.port.out.modulo1.LoteQueryPort lotes) {
    this.galpones = galpones;
    this.lotes = lotes;
  }

  private GalponListadoResult elemento(GalponQueryPort.GalponDatos galpon) {
    var lote = galpon.loteActual();
    if (lote == null && lotes != null) {
      lote =
          lotes
              .consultarActual(galpon.id())
              .map(actual -> new GalponQueryPort.LoteDatos(actual.id(), actual.nombre()))
              .orElse(null);
    }
    return new GalponListadoResult(
        galpon.id(),
        galpon.nombre(),
        galpon.estado(),
        lote == null ? null : new GalponListadoResult.LoteActual(lote.id(), lote.nombre()));
  }

  public ListadoGalponesResult consultar(int pagina, int tamano, String orden) {
    if (pagina < 0 || tamano < 1 || tamano > 100 || pagina > Integer.MAX_VALUE / tamano)
      throw new IllegalArgumentException("Paginación inválida");
    String[] partes = orden == null ? new String[] {"nombre", "asc"} : orden.split(",", -1);
    if (partes.length != 2
        || !CAMPOS.contains(partes[0])
        || !(partes[1].equals("asc") || partes[1].equals("desc")))
      throw new IllegalArgumentException("Orden inválido");
    var resultado = galpones.listar(pagina, tamano, partes[0], partes[1].equals("asc"));
    long paginas = (resultado.totalElementos() + tamano - 1) / tamano;
    return new ListadoGalponesResult(
        resultado.elementos().stream().map(this::elemento).toList(),
        pagina,
        tamano,
        resultado.totalElementos(),
        Math.toIntExact(paginas),
        resultado.totalElementos() == 0 ? "No existen galpones disponibles" : null);
  }
}
