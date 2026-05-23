package com.modelrouter.security;

public record SsoUserDetails(String email, String name, String role, String tier,
                              long trialStartedAt, Long planExpiresAt) {
    public boolean isProActive() {
        return "PRO".equals(tier) && (planExpiresAt == null || planExpiresAt > System.currentTimeMillis());
    }
    public boolean isTrialActive() {
        return System.currentTimeMillis() < trialStartedAt + 7L * 24 * 60 * 60 * 1000;
    }
    public boolean needsUsageCheck() { return !isProActive() && !isTrialActive(); }
}
