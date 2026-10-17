package com.jnta.audit.adapter.out.persistence;

import com.jnta.audit.application.port.out.AuditLogRepository;
import com.jnta.audit.domain.AuditRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaAuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository repository;

    public JpaAuditLogRepositoryAdapter(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(AuditRecord record) {
        repository.save(AuditLogJpaEntity.from(record));
    }
}
