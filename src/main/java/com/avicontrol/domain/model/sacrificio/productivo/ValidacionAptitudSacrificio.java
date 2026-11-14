package com.avicontrol.domain.model.sacrificio.productivo;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.lote.Edad;
import java.util.ArrayList;
import java.util.List;

public record ValidacionAptitudSacrificio(boolean apto, List<String> motivosNoAptitud) {
  public static ValidacionAptitudSacrificio evaluar(String estado, Edad edad) {
    List<String> motivos = new ArrayList<>();
    if (edad.diasTotales() < 45) motivos.add("El lote no cumple la edad mínima de 45 días");
    if (EstadoGalpon.desde(estado).orElse(null) != EstadoGalpon.PRODUCTIVO)
      motivos.add("El galpón no se encuentra en estado productiva");
    return new ValidacionAptitudSacrificio(motivos.isEmpty(), List.copyOf(motivos));
  }
}
