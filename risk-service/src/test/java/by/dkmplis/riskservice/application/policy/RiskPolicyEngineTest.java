package by.dkmplis.riskservice.application.policy;

import by.dkmplis.riskservice.domain.enums.RiskStatus;
import by.dkmplis.riskservice.domain.model.RiskAssessment;
import by.dkmplis.riskservice.domain.policy.RiskCheckResult;
import by.dkmplis.riskservice.domain.policy.RiskDecision;
import by.dkmplis.riskservice.domain.policy.RiskPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RiskPolicyEngineTest {

    private final RiskAssessment assessment =
            new RiskAssessment(
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

    @Test
    void shouldApproveWhenAllPoliciesPass() {
        RiskPolicy firstPolicy =
                mock(RiskPolicy.class);

        RiskPolicy secondPolicy =
                mock(RiskPolicy.class);

        when(firstPolicy.evaluate(assessment))
                .thenReturn(
                        RiskCheckResult.success()
                );

        when(secondPolicy.evaluate(assessment))
                .thenReturn(
                        RiskCheckResult.success()
                );

        RiskPolicyEngine engine =
                new RiskPolicyEngine(
                        List.of(
                                firstPolicy,
                                secondPolicy
                        )
                );

        RiskDecision decision =
                engine.evaluate(
                        assessment
                );

        assertThat(decision.status())
                .isEqualTo(
                        RiskStatus.APPROVED
                );

        assertThat(decision.reason())
                .isEqualTo(
                        "RISK_CHECK_PASSED"
                );

        verify(firstPolicy)
                .evaluate(assessment);

        verify(secondPolicy)
                .evaluate(assessment);
    }

    @Test
    void shouldRejectWhenPolicyRejects() {
        RiskPolicy firstPolicy =
                mock(RiskPolicy.class);

        RiskPolicy secondPolicy =
                mock(RiskPolicy.class);

        when(firstPolicy.evaluate(assessment))
                .thenReturn(
                        RiskCheckResult.success()
                );

        when(secondPolicy.evaluate(assessment))
                .thenReturn(
                        RiskCheckResult.rejected(
                                "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
                        )
                );

        RiskPolicyEngine engine =
                new RiskPolicyEngine(
                        List.of(
                                firstPolicy,
                                secondPolicy
                        )
                );

        RiskDecision decision =
                engine.evaluate(
                        assessment
                );

        assertThat(decision.status())
                .isEqualTo(
                        RiskStatus.REJECTED
                );

        assertThat(decision.reason())
                .isEqualTo(
                        "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
                );
    }

    @Test
    void shouldStopEvaluationAfterFirstRejection() {
        RiskPolicy firstPolicy =
                mock(RiskPolicy.class);

        RiskPolicy rejectingPolicy =
                mock(RiskPolicy.class);

        RiskPolicy thirdPolicy =
                mock(RiskPolicy.class);

        when(firstPolicy.evaluate(assessment))
                .thenReturn(
                        RiskCheckResult.success()
                );

        when(rejectingPolicy.evaluate(assessment))
                .thenReturn(
                        RiskCheckResult.rejected(
                                "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
                        )
                );

        RiskPolicyEngine engine =
                new RiskPolicyEngine(
                        List.of(
                                firstPolicy,
                                rejectingPolicy,
                                thirdPolicy
                        )
                );

        RiskDecision decision =
                engine.evaluate(
                        assessment
                );

        assertThat(decision.status())
                .isEqualTo(
                        RiskStatus.REJECTED
                );

        verify(firstPolicy)
                .evaluate(assessment);

        verify(rejectingPolicy)
                .evaluate(assessment);

        verifyNoInteractions(
                thirdPolicy
        );
    }
}