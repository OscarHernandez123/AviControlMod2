package com.avicontrol.application.galpon.result;

import com.avicontrol.domain.model.lote.Edad;
import java.util.UUID;

public record EdadLoteResult(UUID galponId, UUID loteId, Edad edad, String mensaje) {}
