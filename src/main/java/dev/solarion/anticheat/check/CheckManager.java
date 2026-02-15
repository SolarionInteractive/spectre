package dev.solarion.anticheat.check;

import dev.solarion.anticheat.alert.AlertManager;
import dev.solarion.anticheat.violation.ViolationTracker;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import dev.solarion.anticheat.event.RemoveCheatingPlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.HashSet;
import java.util.Set;

public class CheckManager {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final List<Check> checks = new ArrayList<>();
    private final ViolationTracker violationTracker = new ViolationTracker();
    private final AlertManager alertManager = new AlertManager();
    private final Set<UUID> pendingRemoval = new HashSet<>();
    private final Set<UUID> disconnectingPlayers = new HashSet<>();

    public void registerCheck(Check check) {
        checks.add(check);
    }

    public boolean isDisconnecting(UUID playerId) {
        return disconnectingPlayers.contains(playerId);
    }

    public void markDisconnecting(UUID playerId) {
        disconnectingPlayers.add(playerId);
    }

    public CheckResult runChecks(
            UUID playerId,
            String playerName,
            PlayerRef playerRef,
            PlayerInput.InputUpdate update,
            Vector3d fromPosition,
            Vector3d toPosition,
            MovementStates movementStates,
            MovementManager movementManager,
            float dt,
            boolean inServerVelocity
    ) {
        if (isDisconnecting(playerId)) {
            return CheckResult.passed();
        }

        for (Check check : checks) {
            CheckResult result = check.onMovementUpdate(
                    playerId,
                    playerRef,
                    update,
                    fromPosition,
                    toPosition,
                    movementStates,
                    movementManager,
                    dt,
                    inServerVelocity
            );

            if (result.failed()) {
                handleViolation(playerId, playerName, playerRef, result);
                return result;
            }
        }
        return CheckResult.passed();
    }
    
    public CheckResult checkInputQueue(UUID playerId, int queueSize) {
        for (Check check : checks) {
            CheckResult result = check.onInputQueue(playerId, queueSize);
            if (result.failed()) {
                return result;
            }
        }
        return CheckResult.passed();
    }

    private void handleViolation(UUID playerId, String playerName, PlayerRef playerRef, CheckResult result) {
        if (isDisconnecting(playerId)) {
            return;
        }

        double vl = violationTracker.addViolation(playerId, result);
        
        LOGGER.at(Level.WARNING).log("[Spectre] %s failed %s (VL: %.1f)", playerName, result.getDisplayName(), vl);
        
        alertManager.alert(playerName, playerId, result, vl);
        
        if (violationTracker.shouldKick(vl) && !pendingRemoval.contains(playerId)) {
            pendingRemoval.add(playerId);
            markDisconnecting(playerId);
            removePlayer(playerRef, "Kicked by Spectre: " + result.getDisplayName());
        }
    }
    
    public void onPlayerConnect(UUID playerId) {
        violationTracker.removePlayer(playerId);
        alertManager.removePlayer(playerId);
        pendingRemoval.remove(playerId);
        disconnectingPlayers.remove(playerId);
    }

    public void onPlayerLogout(UUID playerId) {
        violationTracker.removePlayer(playerId);
        alertManager.removePlayer(playerId);
        pendingRemoval.remove(playerId);
        // Do not remove from disconnectingPlayers here, specific fix for race condition
        for (Check check : checks) {
            check.onPlayerLogout(playerId);
        }
    }
    
    public void reportViolation(UUID playerId, String playerName, PlayerRef playerRef, CheckResult result) {
         handleViolation(playerId, playerName, playerRef, result);
    }
    
    public ViolationTracker getViolationTracker() {
        return violationTracker;
    }

    private static void removePlayer(PlayerRef ref, String reason) {
        HytaleServer
                .get()
                .getEventBus()
                .dispatchForAsync(RemoveCheatingPlayerEvent.class)
                .dispatch(new RemoveCheatingPlayerEvent(ref, reason));
    }
}
