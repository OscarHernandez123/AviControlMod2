package com.avicontrol.infrastructure.adapter.in.rest.modulo3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfirmarResultadoUtilizadoRequest(
    @NotNull UUID eventId,
    @NotNull @Pattern(regexp = "ResultadoSacrificioUtilizado") String eventType,
    @NotNull
        @Schema(
            type = "integer",
            allowableValues = {"1"})
        JsonNode eventVersion,
    @NotNull UUID aggregateId,
    @NotNull @Valid Payload payload) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Payload(@NotNull UUID resultadoId, @NotNull UUID loteId) {}
}
