package dev.solarion.anticheat.check.checks;

import dev.solarion.anticheat.check.Check;
import dev.solarion.anticheat.check.CheckResult;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;

public class MantleCheck extends Check {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MAX_MANTLE_TICKS = 30; // ~1 second

    private final Map<UUID, Integer> mantleTickCounts = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "Mantle";
    }

    @Override
    public String getDescription() {
        return "Checks for mantle glitches and spoofing";
    }

    @Override
    public CheckResult onMovementUpdate(
            UUID playerId,
            PlayerRef playerRef,
            PlayerInput.InputUpdate update,
            Vector3d fromPosition,
            Vector3d toPosition,
            MovementStates movementStates,
            MovementManager movementManager,
            float dt,
            boolean inServerVelocity
    ) {
        if (update instanceof PlayerInput.AbsoluteMovement) {
             if (movementStates.onGround && movementStates.mantling) {
                LOGGER.at(Level.WARNING).log("[Mantle] FAILED: Mantle-into-ceiling glitch");
                return CheckResult.fail("Mantle-Into-Ceiling", 5.0);
            }
        }

        if (update instanceof PlayerInput.SetMovementStates(MovementStates newStates)) {
            if (newStates.mantling) {
                int mantleTicks = mantleTickCounts.getOrDefault(playerId, 0) + 1;
                mantleTickCounts.put(playerId, mantleTicks);

                if (mantleTicks > MAX_MANTLE_TICKS) {
                    LOGGER.at(Level.WARNING).log("[Mantle] FAILED: Prolonged mantle state (%d ticks)", mantleTicks);
                    return CheckResult.fail("Spoofed Mantle State", 5.0);
                }
            } else {
                mantleTickCounts.remove(playerId);
            }
        }

        return CheckResult.passed();
    }

    @Override
    public void onPlayerLogout(UUID playerId) {
        mantleTickCounts.remove(playerId);
    }
}
