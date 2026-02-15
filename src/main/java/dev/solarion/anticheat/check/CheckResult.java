package dev.solarion.anticheat.check;

/**
 * Result of a check execution
 */
public record CheckResult(boolean failed, String reason, double vlWeight) {

    public static CheckResult passed() {
        return new CheckResult(false, "Passed", 0.0);
    }

    public static CheckResult fail(String reason, double vlWeight) {
        return new CheckResult(true, reason, vlWeight);
    }

    public String getDisplayName() {
        return reason;
    }
    
    public double getVlWeight() {
        return vlWeight;
    }
}
