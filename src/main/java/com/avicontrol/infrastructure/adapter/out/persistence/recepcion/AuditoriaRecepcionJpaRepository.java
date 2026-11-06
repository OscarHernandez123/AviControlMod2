package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRecepcionJpaRepository
    extends JpaRepository<AuditoriaRecepcionEntity, UUID> {}
