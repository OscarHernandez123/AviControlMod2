package com.avicontrol.infrastructure.adapter.out.persistence.catalogo;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AlimentoJpaRepository extends JpaRepository<AlimentoJpaEntity, UUID> {}
