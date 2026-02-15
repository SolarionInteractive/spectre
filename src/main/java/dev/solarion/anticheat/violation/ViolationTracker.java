package dev.solarion.anticheat.violation;

import dev.solarion.anticheat.check.CheckResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player violation levels and decay
 */
public class ViolationTracker {

    public static final double KICK_THRESHOLD = 20.0;
    public static final double DECAY_PER_TICK = 0.05;

    private final Map<UUID, Double> violationLevels = new ConcurrentHashMap<>();

    // Adds violation and returns new VL
    public double addViolation(UUID playerId, CheckResult result) {
        if (!result.failed()) return getViolationLevel(playerId);
        return violationLevels.merge(playerId, result.getVlWeight(), Double::sum);
    }


    public double getViolationLevel(UUID playerId) {
        return violationLevels.getOrDefault(playerId, 0.0);
    }

    // Decays VL by fixed amount per tick
    public void decay(UUID playerId) {
        violationLevels.computeIfPresent(playerId, (ignored, vl) -> {
            vl -= DECAY_PER_TICK;
            return vl <= 0 ? null : vl;
        });
    }


    public void removePlayer(UUID playerId) {
        violationLevels.remove(playerId);
    }

    public boolean shouldKick(double violationLevel) {
        return violationLevel >= KICK_THRESHOLD;
    }
}
