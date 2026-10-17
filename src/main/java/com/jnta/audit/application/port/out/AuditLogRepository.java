package com.jnta.audit.application.port.out;

import com.jnta.audit.domain.AuditRecord;

public interface AuditLogRepository {

    void append(AuditRecord record);
}
