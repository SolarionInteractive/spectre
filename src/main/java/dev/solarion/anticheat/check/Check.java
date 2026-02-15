package dev.solarion.anticheat.check;

import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.UUID;

public abstract class Check {

    public abstract String getName();
    public abstract String getDescription();

     public abstract CheckResult onMovementUpdate(
            UUID playerId,
            PlayerRef playerRef,
            PlayerInput.InputUpdate update,
            Vector3d fromPosition,
            Vector3d toPosition,
            MovementStates movementStates,
            MovementManager movementManager,
            float dt,
            boolean inServerVelocity
    );

    public CheckResult onInputQueue(UUID playerId, int queueSize) {
        return CheckResult.passed();
    }

    public void onPlayerLogout(UUID playerId) {}
}
