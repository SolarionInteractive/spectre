package dev.solarion.anticheat.check.checks;

import dev.solarion.anticheat.check.Check;
import dev.solarion.anticheat.check.CheckResult;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.UUID;
import java.util.logging.Level;

public class RelativeSpeedCheck extends Check {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final double MAX_RELATIVE_MOVEMENT = 2.0;

    @Override
    public String getName() {
        return "RelativeSpeed";
    }

    @Override
    public String getDescription() {
        return "Checks for excessive relative movement";
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
        if (update instanceof PlayerInput.RelativeMovement relativeMovement) {
            double magnitude = Math.sqrt(
                    relativeMovement.getX() * relativeMovement.getX() +
                    relativeMovement.getY() * relativeMovement.getY() +
                    relativeMovement.getZ() * relativeMovement.getZ()
            );

            if (magnitude > MAX_RELATIVE_MOVEMENT) {
                LOGGER.at(Level.WARNING).log("[RelativeSpeed] FAILED: Relative movement magnitude %.2f > %.2f", magnitude, MAX_RELATIVE_MOVEMENT);
                return CheckResult.fail("Relative Movement Speed", 3.0);
            }
        }
        return CheckResult.passed();
    }
}
