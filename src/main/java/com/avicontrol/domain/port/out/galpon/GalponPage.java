package com.avicontrol.domain.port.out.galpon;

import com.avicontrol.domain.model.galpon.Galpon;
import java.util.List;

public record GalponPage(List<Galpon> content, long totalElements) {}
