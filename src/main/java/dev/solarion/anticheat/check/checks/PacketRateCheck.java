package dev.solarion.anticheat.check.checks;

import dev.solarion.anticheat.check.Check;
import dev.solarion.anticheat.check.CheckResult;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.UUID;

public class PacketRateCheck extends Check {

    private static final int MAX_UPDATES_PER_TICK = 20;

    @Override
    public String getName() {
        return "PacketRate";
    }

    @Override
    public String getDescription() {
        return "Checks for excessive packet rate";
    }

    @Override
    public CheckResult onInputQueue(UUID playerId, int queueSize) {
        if (queueSize > MAX_UPDATES_PER_TICK) {
            return CheckResult.fail("Packet Rate Exceeded", 3.0);
        }
        return CheckResult.passed();
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
        return CheckResult.passed();
    }
}
