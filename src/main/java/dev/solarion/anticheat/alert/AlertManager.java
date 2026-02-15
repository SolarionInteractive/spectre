package dev.solarion.anticheat.alert;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.solarion.anticheat.check.CheckResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages throttled staff alerts for violations
 */
public class AlertManager {

    private static final String ALERT_PERMISSION = "solarion.anticheat.alerts";
    private static final long ALERT_COOLDOWN_MS = 2000;

    private final Map<UUID, Long> lastAlertTime = new ConcurrentHashMap<>();

    // Alerts staff (throttled)
    public void alert(String playerName, UUID playerId, CheckResult result, double violationLevel) {
        long now = System.currentTimeMillis();
        Long lastTime = lastAlertTime.get(playerId);
        if (lastTime != null && (now - lastTime) < ALERT_COOLDOWN_MS) {
            return;
        }
        lastAlertTime.put(playerId, now);

        Message alertMessage = Message.raw(
                String.format("[Spectre] %s failed %s (VL: %.1f)", playerName, result.getDisplayName(), violationLevel)
        ).color("#ff5555");

        for (PlayerRef ref : Universe.get().getPlayers()) {
            try {
                if (PermissionsModule.get().hasPermission(ref.getUuid(), ALERT_PERMISSION)) {
                    ref.sendMessage(alertMessage);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void removePlayer(UUID playerId) {
        lastAlertTime.remove(playerId);
    }
}
