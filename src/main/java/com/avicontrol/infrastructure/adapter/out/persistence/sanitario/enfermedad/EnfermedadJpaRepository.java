package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.enfermedad;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EnfermedadJpaRepository extends JpaRepository<EnfermedadJpaEntity, UUID> {}
