package com.jnta.audit.adapter.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnta.audit.application.annotation.Audit;
import com.jnta.audit.application.port.out.AuditLogRepository;
import com.jnta.audit.domain.AuditAction;
import com.jnta.audit.domain.AuditRecord;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.command.DecideSolicitationCommand;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.review.Decision;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditAspectTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final AuditAspect aspect = new AuditAspect(
            auditLogRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldRecordSuccessfulSolicitationSubmission() throws Throwable {
        UUID solicitationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        var solicitation = Solicitation.create(new SolicitationId(solicitationId), new UserId(actorId));
        var joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {
                new SolicitationId(solicitationId), new UserId(actorId)
        });
        when(joinPoint.proceed()).thenReturn(solicitation);

        Object result = aspect.record(joinPoint, annotation("submit"));

        assertThat(result).isSameAs(solicitation);
        var record = capturedRecord();
        assertThat(record.actorId()).isEqualTo(new UserId(actorId));
        assertThat(record.action()).isEqualTo(AuditAction.SOLICITATION_SUBMITTED);
        assertThat(record.aggregateType()).isEqualTo("Solicitation");
        assertThat(record.aggregateId()).isEqualTo(solicitationId);
        assertThat(record.occurredAt()).isEqualTo(NOW);
        assertThat(record.success()).isTrue();
    }

    @Test
    void shouldRecordFailedOperationAndRethrowFailure() throws Throwable {
        UUID solicitationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        var failure = new IllegalStateException("submission failed");
        var joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {
                new SolicitationId(solicitationId), new UserId(actorId)
        });
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.record(joinPoint, annotation("submit")))
                .isSameAs(failure);

        var record = capturedRecord();
        assertThat(record.action()).isEqualTo(AuditAction.SOLICITATION_SUBMITTED);
        assertThat(record.aggregateId()).isEqualTo(solicitationId);
        assertThat(record.success()).isFalse();
    }

    @Test
    void shouldRecordTheDecisionOutcomeAsTheAction() throws Throwable {
        UUID solicitationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        var command = new DecideSolicitationCommand(Decision.Outcome.APPROVE, "Reviewed");
        var joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {
                new SolicitationId(solicitationId), new UserId(actorId), command
        });
        when(joinPoint.proceed()).thenReturn(null);

        aspect.record(joinPoint, annotation("decide"));

        assertThat(capturedRecord().action()).isEqualTo(AuditAction.SOLICITATION_APPROVED);
    }

    private AuditRecord capturedRecord() {
        var captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogRepository).append(captor.capture());
        return captor.getValue();
    }

    private static Audit annotation(String methodName) throws NoSuchMethodException {
        return AuditedOperations.class.getDeclaredMethod(methodName).getAnnotation(Audit.class);
    }

    private static class AuditedOperations {

        @Audit(action = AuditAction.SOLICITATION_SUBMITTED)
        void submit() {
        }

        @Audit(action = AuditAction.SOLICITATION_DECIDED)
        void decide() {
        }
    }
}
