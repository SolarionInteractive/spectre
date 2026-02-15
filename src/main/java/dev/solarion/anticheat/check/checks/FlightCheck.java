package dev.solarion.anticheat.check.checks;

import dev.solarion.anticheat.check.Check;
import dev.solarion.anticheat.check.CheckResult;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.UUID;

public class FlightCheck extends Check {

    @Override
    public String getName() {
        return "Flight";
    }

    @Override
    public String getDescription() {
        return "Checks if player is flying";
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
        if (update instanceof PlayerInput.SetMovementStates(MovementStates states)) {
            if (states.flying) {
                return CheckResult.fail("Flying", 10.0);
            }
        }
        return CheckResult.passed();
    }
}
