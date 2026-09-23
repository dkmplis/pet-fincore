package by.dkmplis.riskservice.domain.policy;

import by.dkmplis.riskservice.domain.model.RiskAssessment;

public interface RiskPolicy {
    RiskCheckResult evaluate(RiskAssessment assessment);
}
