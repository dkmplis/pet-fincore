package by.dkmplis.riskservice.application.policy;

import by.dkmplis.riskservice.domain.model.RiskAssessment;
import by.dkmplis.riskservice.domain.policy.RiskCheckResult;
import by.dkmplis.riskservice.infrastructure.config.RiskPolicyProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class MaxTransferPolicyTest {

    private static final long MAX_AMOUNT_MINOR = 500_000L;

    private final MaxTransferPolicy policy =
            new MaxTransferPolicy(
                    new RiskPolicyProperties(
                            MAX_AMOUNT_MINOR
                    )
            );

    @Test
    void shouldPassWhenAmountIsBelowLimit() {
        RiskAssessment assessment =
                assessmentWithAmount(
                        499_999L
                );

        RiskCheckResult result =
                policy.evaluate(
                        assessment
                );

        assertThat(result.passed())
                .isTrue();

        assertThat(result.reason())
                .isNull();
    }

    @Test
    void shouldPassWhenAmountEqualsLimit() {
        RiskAssessment assessment =
                assessmentWithAmount(
                        MAX_AMOUNT_MINOR
                );

        RiskCheckResult result =
                policy.evaluate(
                        assessment
                );

        assertThat(result.passed())
                .isTrue();
    }

    @Test
    void shouldRejectWhenAmountExceedsLimit() {
        RiskAssessment assessment =
                assessmentWithAmount(
                        500_001L
                );

        RiskCheckResult result =
                policy.evaluate(
                        assessment
                );

        assertThat(result.passed())
                .isFalse();

        assertThat(result.reason())
                .isEqualTo(
                        "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
                );
    }

    private RiskAssessment assessmentWithAmount(
            long amountMinor
    ) {
        return new RiskAssessment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BYN",
                amountMinor
        );
    }
}