package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegistroAuditoriaRecepcionJpaRepository
    extends JpaRepository<RegistroAuditoriaRecepcionJpaEntity, UUID> {}
