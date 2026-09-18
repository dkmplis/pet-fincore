package by.dkmplis.riskservice.domain.policy;

public record RiskCheckResult(
        boolean passed,
        String reason
) {
    public static RiskCheckResult rejected(
            String reason
    ) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required"
            );
        }

        return new RiskCheckResult(
                false,
                reason
        );
    }

    public static RiskCheckResult success() {
        return new RiskCheckResult(
                true,
                null
        );
    }

}
