package com.jnta.outbox.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    List<OutboxEventJpaEntity> findByProcessedAtIsNullOrderByCreatedAtAsc(Pageable pageable);

    long countByProcessedAtIsNull();
}
