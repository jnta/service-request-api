package com.jnta.audit.adapter.aop;

import com.jnta.audit.application.annotation.Audit;
import com.jnta.audit.application.port.out.AuditLogRepository;
import com.jnta.audit.domain.AuditAction;
import com.jnta.audit.domain.AuditRecord;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.command.DecideSolicitationCommand;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private static final String AGGREGATE_TYPE = "Solicitation";

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditAspect(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Around(value = "@annotation(audit)", argNames = "joinPoint,audit")
    public Object record(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable failure) {
            try {
                append(joinPoint, audit, null, false);
            } catch (RuntimeException auditFailure) {
                failure.addSuppressed(auditFailure);
            }
            throw failure;
        }
        append(joinPoint, audit, result, true);
        return result;
    }

    private void append(ProceedingJoinPoint joinPoint, Audit audit, Object result, boolean success) {
        var actorId = Arrays.stream(joinPoint.getArgs())
                .filter(UserId.class::isInstance)
                .map(UserId.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Audited operation requires an actor"));
        UUID aggregateId = Arrays.stream(joinPoint.getArgs())
                .filter(SolicitationId.class::isInstance)
                .map(SolicitationId.class::cast)
                .map(SolicitationId::value)
                .findFirst()
                .orElseGet(() -> result instanceof Solicitation solicitation
                        ? solicitation.id().value()
                        : null);

        auditLogRepository.append(new AuditRecord(
                UUID.randomUUID(),
                actorId,
                resolveAction(audit.action(), joinPoint.getArgs()),
                AGGREGATE_TYPE,
                aggregateId,
                clock.instant(),
                success));
    }

    private static AuditAction resolveAction(AuditAction action, Object[] arguments) {
        if (action != AuditAction.SOLICITATION_DECIDED) {
            return action;
        }
        return Arrays.stream(arguments)
                .filter(DecideSolicitationCommand.class::isInstance)
                .map(DecideSolicitationCommand.class::cast)
                .map(DecideSolicitationCommand::decision)
                .filter(decision -> decision != null)
                .map(decision -> switch (decision) {
                    case APPROVE -> AuditAction.SOLICITATION_APPROVED;
                    case REJECT -> AuditAction.SOLICITATION_REJECTED;
                })
                .findFirst()
                .orElse(AuditAction.SOLICITATION_DECIDED);
    }
}
