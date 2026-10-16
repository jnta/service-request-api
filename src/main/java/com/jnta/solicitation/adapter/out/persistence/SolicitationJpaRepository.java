package com.jnta.solicitation.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitationJpaRepository extends JpaRepository<SolicitationJpaEntity, UUID> {
}
