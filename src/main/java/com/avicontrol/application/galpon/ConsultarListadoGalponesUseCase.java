package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.GalponListadoResult;
import com.avicontrol.application.galpon.result.ListadoGalponesResult;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.Set;

public class ConsultarListadoGalponesUseCase {
  private static final Set<String> CAMPOS = Set.of("nombre", "estado", "id");
  private final GalponQueryPort galpones;

  public ConsultarListadoGalponesUseCase(GalponQueryPort galpones) {
    this.galpones = galpones;
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
        resultado.elementos().stream()
            .map(g -> new GalponListadoResult(g.id(), g.nombre(), g.estado()))
            .toList(),
        pagina,
        tamano,
        resultado.totalElementos(),
        Math.toIntExact(paginas),
        resultado.totalElementos() == 0 ? "No existen galpones disponibles" : null);
  }
}
