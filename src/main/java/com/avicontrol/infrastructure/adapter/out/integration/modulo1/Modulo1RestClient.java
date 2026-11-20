package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente de solo lectura. Un 404 de detalle es ausencia; cualquier otro fallo es indisponibilidad.
 */
public class Modulo1RestClient {
  private final URI baseUrl;
  private final Duration timeout;
  private final HttpClient http;
  private final JsonMapper json =
      JsonMapper.builder()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .build();

  public Modulo1RestClient(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
    if (baseUrl == null
        || !List.of("http", "https").contains(baseUrl.getScheme())
        || baseUrl.getHost() == null
        || baseUrl.getQuery() != null
        || baseUrl.getFragment() != null
        || baseUrl.getUserInfo() != null
        || connectTimeout.isNegative()
        || connectTimeout.isZero()
        || readTimeout.isNegative()
        || readTimeout.isZero()) {
      throw new IllegalArgumentException("Configuración REST del Módulo 1 inválida");
    }
    this.baseUrl = URI.create(baseUrl.toString().replaceAll("/+$", ""));
    this.timeout = readTimeout;
    this.http = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
  }

  public Optional<Galpon> detalle(UUID id) {
    try {
      return get("/api/v1/galpones/" + id, Galpon.class, true)
          .map(
              g -> {
                validar(g);
                if (!id.equals(g.id()))
                  throw new IllegalArgumentException("Identificador inesperado");
                return g;
              });
    } catch (RuntimeException e) {
      throw new InformacionGalponNoDisponibleException();
    }
  }

  public List<Galpon> todos() {
    try {
      List<Galpon> galpones = new ArrayList<>();
      int pagina = 1;
      int paginas;
      do {
        Pagina respuesta =
            get("/api/v1/galpones?page=" + pagina + "&orden=nombre,asc", Pagina.class, false)
                .orElseThrow();
        if (respuesta.contenido() == null
            || respuesta.pagina() == null
            || respuesta.pagina() != pagina
            || respuesta.totalPaginas() == null
            || respuesta.totalPaginas() < 0
            || respuesta.tamanio() == null
            || respuesta.tamanio() != 10
            || respuesta.totalElementos() == null
            || respuesta.totalElementos() < 0
            || respuesta.totalPaginas() != (respuesta.totalElementos() + 9L) / 10L
            || respuesta.contenido().size() > 10
            || (pagina < respuesta.totalPaginas() && respuesta.contenido().size() != 10)) {
          throw new IllegalArgumentException("Paginación remota inválida");
        }
        respuesta.contenido().forEach(Modulo1RestClient::validar);
        galpones.addAll(respuesta.contenido());
        paginas = respuesta.totalPaginas();
        if (pagina >= paginas && galpones.size() != respuesta.totalElementos())
          throw new IllegalArgumentException("Listado remoto inconsistente");
        pagina++;
      } while (pagina <= paginas);
      if (galpones.stream().map(Galpon::id).distinct().count() != galpones.size())
        throw new IllegalArgumentException("Galpones duplicados");
      return List.copyOf(galpones);
    } catch (RuntimeException e) {
      throw new InformacionGalponNoDisponibleException();
    }
  }

  private <T> Optional<T> get(String path, Class<T> type, boolean permiteAusencia) {
    try {
      var request =
          HttpRequest.newBuilder(URI.create(baseUrl + path))
              .timeout(timeout)
              .header("Accept", "application/json")
              .GET()
              .build();
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 404 && permiteAusencia) return Optional.empty();
      if (response.statusCode() != 200) throw new InformacionGalponNoDisponibleException();
      return Optional.of(json.readValue(response.body(), type));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InformacionGalponNoDisponibleException();
    } catch (Exception e) {
      throw new InformacionGalponNoDisponibleException();
    }
  }

  private static void validar(Galpon g) {
    if (g == null
        || g.id() == null
        || g.nombre() == null
        || g.estado() == null
        || g.capacidad() == null
        || g.capacidad() < 0) throw new IllegalArgumentException("Galpón inválido");
    // Los puertos actuales usan int: nunca truncar silenciosamente la capacidad de 64 bits.
    Math.toIntExact(g.capacidad());
    var lote = g.loteActivo();
    if (lote != null
        && (lote.id() == null || lote.poblacionActual() == null || lote.poblacionActual() < 0))
      throw new IllegalArgumentException("Lote inválido");
  }

  public record Galpon(
      UUID id, String nombre, Long capacidad, String estado, LoteActivo loteActivo) {}

  public record LoteActivo(
      UUID id, String nombre, Integer poblacionActual, LocalDate fechaIngreso) {}

  public record Pagina(
      List<Galpon> contenido,
      Integer pagina,
      Integer tamanio,
      Long totalElementos,
      Integer totalPaginas) {}
}
