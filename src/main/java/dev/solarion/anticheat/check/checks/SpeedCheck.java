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

public class SpeedCheck extends Check {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final double MAX_STEP_HEIGHT = 1.1;
    public static final double MAX_FALLING_SPEED = 60.0;
    public static final double SPEED_BUFFER_MULTIPLIER = 1.35;
    public static final double VERTICAL_BUFFER_ADDITIVE = 1.35;
    public static final double LATERAL_BUFFER_ADDITIVE = 1.35;
    public static final double COMBAT_MOMENTUM_MULTIPLIER = 1.2;

    @Override
    public String getName() {
        return "Speed";
    }

    @Override
    public String getDescription() {
        return "Checks for invalid lateral and vertical speed";
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
        if (inServerVelocity) {
            return CheckResult.passed();
        }

        if (!(update instanceof PlayerInput.AbsoluteMovement)) {
            return CheckResult.passed();
        }

        // Allow step-up
        if (movementStates.onGround && !movementStates.mantling) {
            double deltaY = toPosition.y - fromPosition.y;
            if (deltaY > 0 && deltaY <= MAX_STEP_HEIGHT) {
                return CheckResult.passed();
            }
        }

        // Allow mantling
        if (movementStates.mantling) {
            return CheckResult.passed();
        }

        var delta = toPosition.clone().subtract(fromPosition);

        // Check horizontal speed
        var lateralDelta = new Vector3d(delta.x, 0, delta.z);
        var lateralSpeed = lateralDelta.length() / dt;
        double lateralLimit = getLateralLimit(movementStates, movementManager);

        if (lateralSpeed > lateralLimit) {
            LOGGER.at(Level.WARNING).log("[Speed] FAILED: Lateral speed %.2f > %.2f", lateralSpeed, lateralLimit);
            return CheckResult.fail("Lateral Speed", 2.0);
        }

        // Check vertical speed (only fail if not falling)
        var verticalSpeed = Math.abs(delta.y) / dt;
        double verticalLimit = getVerticalLimit(movementStates, movementManager, delta.y);

        if (verticalSpeed > verticalLimit && !movementStates.falling) {
            LOGGER.at(Level.WARNING).log("[Speed] FAILED: Vertical speed %.2f > %.2f", verticalSpeed, verticalLimit);
            return CheckResult.fail("Vertical Speed", 2.0);
        }

        return CheckResult.passed();
    }

    private double getVerticalLimit(MovementStates movementStates, MovementManager movementManager, double deltaY) {
        var settings = movementManager.getSettings();
        double maxVerticalSpeed;

        if (movementStates.flying) {
            maxVerticalSpeed = settings.verticalFlySpeed;
        } else if (deltaY > 0) {
            maxVerticalSpeed = settings.jumpForce;
        } else {
            maxVerticalSpeed = MAX_FALLING_SPEED;
        }

        return (maxVerticalSpeed * SPEED_BUFFER_MULTIPLIER) + VERTICAL_BUFFER_ADDITIVE;
    }

    private double getLateralLimit(MovementStates movementStates, MovementManager movementManager) {
        var settings = movementManager.getSettings();
        double maxLateralSpeed;

        if (movementStates.flying) {
            maxLateralSpeed = settings.horizontalFlySpeed;
        } else {
            double forwardMultiplier;
            double strafeMultiplier;

            if (movementStates.crouching) {
                forwardMultiplier = settings.forwardCrouchSpeedMultiplier;
                strafeMultiplier = settings.strafeCrouchSpeedMultiplier;
            } else if (movementStates.sprinting) {
                forwardMultiplier = settings.forwardSprintSpeedMultiplier;
                strafeMultiplier = settings.strafeRunSpeedMultiplier;
            } else if (movementStates.walking) {
                forwardMultiplier = settings.forwardWalkSpeedMultiplier;
                strafeMultiplier = settings.strafeWalkSpeedMultiplier;
            } else {
                forwardMultiplier = settings.forwardRunSpeedMultiplier;
                strafeMultiplier = settings.strafeRunSpeedMultiplier;
            }

            double forwardComponent = settings.baseSpeed * forwardMultiplier;
            double strafeComponent = settings.baseSpeed * strafeMultiplier;
            maxLateralSpeed = Math.hypot(forwardComponent, strafeComponent);

            if (!movementStates.onGround) {
                maxLateralSpeed *= settings.airSpeedMultiplier;
                maxLateralSpeed *= settings.comboAirSpeedMultiplier;
                maxLateralSpeed *= COMBAT_MOMENTUM_MULTIPLIER;
            }
        }

        return (maxLateralSpeed * SPEED_BUFFER_MULTIPLIER) + LATERAL_BUFFER_ADDITIVE;
    }
}
