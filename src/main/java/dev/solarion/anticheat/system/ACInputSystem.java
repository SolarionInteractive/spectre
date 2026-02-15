package dev.solarion.anticheat.system;

import com.hypixel.hytale.builtin.mounts.MountSystems;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.ModelTransform;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.SavedMovementStates;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.player.KnockbackPredictionSystems;
import com.hypixel.hytale.server.core.modules.entity.player.KnockbackSimulation;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import dev.solarion.anticheat.check.CheckManager;
import dev.solarion.anticheat.check.CheckResult;
import dev.solarion.anticheat.check.checks.*;

import javax.annotation.Nonnull;
import java.util.*;

public class ACInputSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    private final Query<EntityStore> query = Query.and(
            Player.getComponentType(),
            PlayerInput.getComponentType(),
            TransformComponent.getComponentType(),
            MovementManager.getComponentType(),
            MovementStatesComponent.getComponentType()
    );

    private final Set<Dependency<EntityStore>> deps = Set.of(
            new SystemDependency<>(Order.BEFORE, PlayerSystems.ProcessPlayerInput.class),
            new SystemDependency<>(Order.BEFORE, PlayerSystems.BlockPausedMovementSystem.class),
            new SystemDependency<>(Order.BEFORE, MountSystems.HandleMountInput.class),
            new SystemDependency<>(Order.BEFORE, DamageSystems.FallDamagePlayers.class),
            new SystemDependency<>(Order.BEFORE, KnockbackPredictionSystems.CaptureKnockbackInput.class)
    );

    private final CheckManager checkManager;

    // Exemption window for server-applied velocity (dashes, launch pads)
    private final Map<UUID, Long> velocityExemptionExpiry = new HashMap<>();
    private static final long VELOCITY_EXEMPTION_DURATION_MS = 600; // covers dash/lunge travel time

    public ACInputSystem(CheckManager checkManager) {
        this.checkManager = checkManager;
        registerChecks();
    }

    private void registerChecks() {
        checkManager.registerCheck(new SpeedCheck());
        checkManager.registerCheck(new RelativeSpeedCheck());
        checkManager.registerCheck(new FlightCheck());
        checkManager.registerCheck(new MantleCheck());
        checkManager.registerCheck(new PacketRateCheck());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return this.deps;
    }

    @Override
    public void tick(float dt,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        var playerInputComponent = archetypeChunk.getComponent(index, PlayerInput.getComponentType());
        var playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
        var transformComponent = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        var movementManager = archetypeChunk.getComponent(index, MovementManager.getComponentType());
        var movementStatesComponent = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());

        assert playerInputComponent != null;
        assert playerComponent != null;
        assert movementManager != null;
        assert transformComponent != null;
        assert movementStatesComponent != null;

        //Bypass for privileged players and creative mode
        if (playerComponent.hasPermission("solarion.anticheat.bypass")) {
            return;
        }
        if (playerComponent.getGameMode() == GameMode.Creative) {
            return;
        }

        // Resolve player identity
        var entityRef = archetypeChunk.getReferenceTo(index);
        var playerRefComponent = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;

        UUID playerId = playerRefComponent.getUuid();
        String playerName = playerRefComponent.getUsername();

        // Decay VL each tick
        checkManager.getViolationTracker().decay(playerId);

        // Check velocity exemption
        boolean inServerVelocity = checkServerVelocity(store, entityRef, playerId);

        var movementUpdateQueue = playerInputComponent.getMovementUpdateQueue();

        // Rate limit check
        CheckResult rateResult = checkManager.checkInputQueue(playerId, movementUpdateQueue.size());
        if (rateResult.failed()) {
            checkManager.reportViolation(playerId, playerName, playerRefComponent, rateResult);
            // Trim excess
            while (movementUpdateQueue.size() > 20) { // Should use constant from PacketRateCheck if public
                movementUpdateQueue.removeLast();
            }
        }

        // Process movement update queue
        List<PlayerInput.InputUpdate> toRemove = new ArrayList<>();
        var virtualPosition = transformComponent.getPosition().clone();
        var currentMovementStates = movementStatesComponent.getMovementStates();

        for (PlayerInput.InputUpdate entry : movementUpdateQueue) {
            
            Vector3d toPosition = virtualPosition.clone();
            
            if (entry instanceof PlayerInput.AbsoluteMovement abs) {
                toPosition = new Vector3d(abs.getX(), abs.getY(), abs.getZ());
            } else if (entry instanceof PlayerInput.RelativeMovement rel) {
                toPosition.add(rel.getX(), rel.getY(), rel.getZ());
            }

            CheckResult result = checkManager.runChecks(
                    playerId,
                    playerName,
                    playerRefComponent,
                    entry,
                    virtualPosition,
                    toPosition,
                    currentMovementStates,
                    movementManager,
                    dt,
                    inServerVelocity
            );

            if (result.failed()) {
                cancelAndCorrect(archetypeChunk, commandBuffer, index, virtualPosition);
                toRemove.add(entry);
                // Don't update virtualPosition if failed/cancelled
            } else {
                // Update virtual position/state for next iteration
                if (entry instanceof PlayerInput.AbsoluteMovement) {
                    virtualPosition = toPosition;
                } else if (entry instanceof PlayerInput.RelativeMovement) {
                    virtualPosition = toPosition;
                } else if (entry instanceof PlayerInput.SetMovementStates(
                        MovementStates movementStates
                )) {
                    currentMovementStates = movementStates;
                }
            }
        }

        movementUpdateQueue.removeAll(toRemove);
    }
    
    private boolean checkServerVelocity(Store<EntityStore> store, Ref<EntityStore> entityRef, UUID playerId) {
        var knockbackSim = store.getComponent(entityRef, KnockbackSimulation.getComponentType());
        var velocityComponent = store.getComponent(entityRef, Velocity.getComponentType());
        boolean hasPendingVelocity = velocityComponent != null && !velocityComponent.getInstructions().isEmpty();

        if (hasPendingVelocity) {
            velocityExemptionExpiry.put(playerId, System.currentTimeMillis() + VELOCITY_EXEMPTION_DURATION_MS);
        }

        long now = System.currentTimeMillis();
        Long exemptExpiry = velocityExemptionExpiry.get(playerId);
        boolean inVelocityExemption = exemptExpiry != null && now < exemptExpiry;
        if (exemptExpiry != null && now >= exemptExpiry) {
            velocityExemptionExpiry.remove(playerId);
        }

        return knockbackSim != null || hasPendingVelocity || inVelocityExemption;
    }

    //Teleports client to valid position and resets state
    private static void cancelAndCorrect(@Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                                          @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                          int index,
                                          @Nonnull Vector3d correctedPosition) {
        var transformComponent = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        var headRotationComponent = archetypeChunk.getComponent(index, HeadRotation.getComponentType());

        if (transformComponent == null || headRotationComponent == null) {
            return;
        }

        var playerRef = archetypeChunk.getReferenceTo(index);
        var rotation = transformComponent.getRotation();
        var headRotation = headRotationComponent.getRotation();

        var playerRefComponent = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        // Teleport to the last known valid position (not the tick-start position)
        var teleportPacket = new ClientTeleport(
                (byte) 0,
                new ModelTransform(
                        PositionUtil.toPositionPacket(correctedPosition),
                        PositionUtil.toDirectionPacket(rotation),
                        PositionUtil.toDirectionPacket(headRotation)
                ),
                false
        );
        var statePacket = new SetMovementStates(new SavedMovementStates(false));

        if (playerRefComponent != null) {
            playerRefComponent.getPacketHandler().write(teleportPacket);
            playerRefComponent.getPacketHandler().write(statePacket);
        }
    }
}
