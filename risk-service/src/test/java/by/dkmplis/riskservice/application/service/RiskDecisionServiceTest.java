package by.dkmplis.riskservice.application.service;

import by.dkmplis.riskservice.application.policy.RiskPolicyEngine;
import by.dkmplis.riskservice.domain.enums.RiskStatus;
import by.dkmplis.riskservice.domain.model.RiskAssessment;
import by.dkmplis.riskservice.domain.policy.RiskDecision;
import by.dkmplis.riskservice.infrastructure.persistence.RiskAssessmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RiskDecisionServiceTest {

    private RiskAssessmentRepository repository;
    private RiskPolicyEngine policyEngine;

    private RiskDecisionService service;

    @BeforeEach
    void setUp() {
        repository =
                mock(
                        RiskAssessmentRepository.class
                );

        policyEngine =
                mock(
                        RiskPolicyEngine.class
                );

        service =
                new RiskDecisionService(
                        repository,
                        policyEngine
                );
    }

    @Test
    void shouldApprovePendingAssessment() {
        RiskAssessment assessment =
                createPendingAssessment();

        when(repository.findByIdForUpdate(
                assessment.getId()
        ))
                .thenReturn(
                        Optional.of(assessment)
                );

        when(policyEngine.evaluate(assessment))
                .thenReturn(
                        new RiskDecision(
                                RiskStatus.APPROVED,
                                "RISK_CHECK_PASSED"
                        )
                );

        RiskStatus result =
                service.decide(
                        assessment.getId()
                );

        assertThat(result)
                .isEqualTo(
                        RiskStatus.APPROVED
                );

        assertThat(assessment.getStatus())
                .isEqualTo(
                        RiskStatus.APPROVED
                );

        assertThat(assessment.getDecisionReason())
                .isEqualTo(
                        "RISK_CHECK_PASSED"
                );

        assertThat(assessment.getDecidedAt())
                .isNotNull();

        assertThat(assessment.getUpdatedAt())
                .isEqualTo(
                        assessment.getDecidedAt()
                );
    }

    @Test
    void shouldRejectPendingAssessment() {
        RiskAssessment assessment =
                createPendingAssessment();

        when(repository.findByIdForUpdate(
                assessment.getId()
        ))
                .thenReturn(
                        Optional.of(assessment)
                );

        when(policyEngine.evaluate(assessment))
                .thenReturn(
                        new RiskDecision(
                                RiskStatus.REJECTED,
                                "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
                        )
                );

        RiskStatus result =
                service.decide(
                        assessment.getId()
                );

        assertThat(result)
                .isEqualTo(
                        RiskStatus.REJECTED
                );

        assertThat(assessment.getStatus())
                .isEqualTo(
                        RiskStatus.REJECTED
                );

        assertThat(assessment.getDecisionReason())
                .isEqualTo(
                        "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
                );

        assertThat(assessment.getDecidedAt())
                .isNotNull();
    }

    @Test
    void shouldReturnExistingDecisionWithoutEvaluatingAgain() {
        RiskAssessment assessment =
                createPendingAssessment();

        assessment.approve(
                "RISK_CHECK_PASSED",
                java.time.Instant.now()
        );

        when(repository.findByIdForUpdate(
                assessment.getId()
        ))
                .thenReturn(
                        Optional.of(assessment)
                );

        RiskStatus result =
                service.decide(
                        assessment.getId()
                );

        assertThat(result)
                .isEqualTo(
                        RiskStatus.APPROVED
                );

        verifyNoInteractions(
                policyEngine
        );
    }

    @Test
    void shouldThrowWhenAssessmentDoesNotExist() {
        UUID assessmentId =
                UUID.randomUUID();

        when(repository.findByIdForUpdate(
                assessmentId
        ))
                .thenReturn(
                        Optional.empty()
                );

        assertThatThrownBy(
                () -> service.decide(
                        assessmentId
                )
        )
                .isInstanceOf(
                        EntityNotFoundException.class
                )
                .hasMessageContaining(
                        assessmentId.toString()
                );

        verifyNoInteractions(
                policyEngine
        );
    }

    private RiskAssessment createPendingAssessment() {
        return new RiskAssessment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                ),
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000002"
                ),
                "BYN",
                10_000L
        );
    }
}