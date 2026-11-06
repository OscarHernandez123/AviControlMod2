package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecepcionAlimentoJpaRepository
    extends JpaRepository<RecepcionAlimentoEntity, UUID>,
        JpaSpecificationExecutor<RecepcionAlimentoEntity> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from RecepcionAlimentoEntity r where r.id = :id")
  Optional<RecepcionAlimentoEntity> bloquear(@Param("id") UUID id);

  @Query(
      "select count(r) from RecepcionAlimentoEntity r where r.fechaIngreso >= :desde and r.fechaIngreso < :hasta and r.estado = 'CONFIRMADA'")
  long contarMes(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

  @Query(
      "select coalesce(sum(r.cantidadTotalKg), 0) from RecepcionAlimentoEntity r where r.fechaIngreso >= :desde and r.fechaIngreso < :hasta and r.estado = 'CONFIRMADA'")
  BigDecimal pesoMes(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
