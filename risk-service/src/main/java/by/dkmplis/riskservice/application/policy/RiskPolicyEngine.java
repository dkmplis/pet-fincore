package by.dkmplis.riskservice.application.policy;

import by.dkmplis.riskservice.domain.enums.RiskStatus;
import by.dkmplis.riskservice.domain.model.RiskAssessment;
import by.dkmplis.riskservice.domain.policy.RiskCheckResult;
import by.dkmplis.riskservice.domain.policy.RiskDecision;
import by.dkmplis.riskservice.domain.policy.RiskPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RiskPolicyEngine {

    private final List<RiskPolicy> policies;
    private final static String PASSED = "RISK_CHECK_PASSED";

    public RiskDecision evaluate(
            RiskAssessment assessment
    ) {
        for (RiskPolicy policy : policies) {
            RiskCheckResult result = policy.evaluate(assessment);
            if (!result.passed()) {
                return new RiskDecision(
                        RiskStatus.REJECTED,
                        result.reason()
                );
            }
        }

        return new RiskDecision(
                RiskStatus.APPROVED,
                PASSED
        );
    }
}

