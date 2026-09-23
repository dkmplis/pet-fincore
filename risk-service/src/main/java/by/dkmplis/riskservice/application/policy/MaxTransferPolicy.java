package by.dkmplis.riskservice.application.policy;

import by.dkmplis.riskservice.domain.model.RiskAssessment;
import by.dkmplis.riskservice.domain.policy.RiskCheckResult;
import by.dkmplis.riskservice.domain.policy.RiskPolicy;
import by.dkmplis.riskservice.infrastructure.config.RiskPolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaxTransferPolicy implements RiskPolicy {

    private final RiskPolicyProperties properties;

    public RiskCheckResult evaluate(
            RiskAssessment assessment
    ) {
        if (assessment.getAmountMinor()
                > properties.maxTransferAmountMinor()) {

            return RiskCheckResult.rejected(
                    "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
            );
        }

        return RiskCheckResult.success();
    }
}
