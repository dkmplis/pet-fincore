package by.dkmplis.riskservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "risk.policy"
)
public record RiskPolicyProperties(
        long maxTransferAmountMinor
) {
    public RiskPolicyProperties {
        if (maxTransferAmountMinor <= 0) {
            throw new IllegalArgumentException(
                    "Risk max transfer amount must be positive"
            );
        }
    }
}
