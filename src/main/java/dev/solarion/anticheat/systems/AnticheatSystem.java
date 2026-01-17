package dev.solarion.anticheat.systems;

import com.hypixel.hytale.builtin.mounts.MountSystems;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.player.KnockbackPredictionSystems;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import dev.solarion.anticheat.events.RemoveCheatingPlayerEvent;
import com.hypixel.hytale.math.vector.Vector3d;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;

public class AnticheatSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static class ScanPlayerInput extends EntityTickingSystem<EntityStore> {
        @Nonnull
        private final Query<EntityStore> query = Query.and(Player.getComponentType(), PlayerInput.getComponentType(), TransformComponent.getComponentType());
        private final Set<Dependency<EntityStore>> deps = Set.of(
                new SystemDependency<>(Order.BEFORE, PlayerSystems.ProcessPlayerInput.class),
                new SystemDependency<>(Order.BEFORE, PlayerSystems.BlockPausedMovementSystem.class),
                new SystemDependency<>(Order.BEFORE, MountSystems.HandleMountInput.class),
                new SystemDependency<>(Order.BEFORE, DamageSystems.FallDamagePlayers.class),
                new SystemDependency<>(Order.BEFORE, KnockbackPredictionSystems.CaptureKnockbackInput.class)
        );

        public ScanPlayerInput() {
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
            var movementStatesComponent = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());

            assert playerInputComponent != null;
            assert playerComponent != null;

//            LOGGER.atInfo().log("Starting anticheat scan tick for " + playerComponent.getDisplayName());

            if (playerComponent.hasPermission("solarion.anticheat.bypass")) {
//                LOGGER.atInfo().log("    - Player bypassed anticheat scan due to permission bypass");
                return;

            } else if (playerComponent.getGameMode() == GameMode.Creative) {
//                LOGGER.atInfo().log("    - Player bypassed anticheat scan due to creative mode");
                return;
            }

            var movementUpdateQueue = playerInputComponent.getMovementUpdateQueue();
            List<PlayerInput.InputUpdate> toRemove = new ArrayList<>();

            for (PlayerInput.InputUpdate entry : movementUpdateQueue) {
                switch (entry) {
                    case PlayerInput.RelativeMovement relativeMovement:
                        // possibly not used?
                        break;
                    case PlayerInput.AbsoluteMovement absoluteMovement:
                        var moveCheckFailed = checkInvalidAbsoluteMovementPacket(absoluteMovement, transformComponent, movementStatesComponent, dt);
                        if (moveCheckFailed) {
                            cancelInputUpdate(entry, toRemove, archetypeChunk, store, commandBuffer, index);
                            // TODO: emit event for failed anitcheat check
                        }
                        break;
                    case PlayerInput.SetMovementStates movementStates:
                        if (movementStates.movementStates().flying) {
                            cancelInputUpdate(entry, toRemove, archetypeChunk, store, commandBuffer, index);
                            // TODO: emit event for failed anitcheat check
//                            var playerRef = archetypeChunk.getReferenceTo(index);
//                            var playerRefComponent = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
//                            kickPlayer(playerRefComponent, "Kicked for flying");
                        }
                        break;
                    default:
                }
            }

            movementUpdateQueue.removeAll(toRemove);
        }
    }

    // TODO: figure out how to kick player from server in an ECS System
    private static void kickPlayer(PlayerRef ref, String reason) {
        HytaleServer
                .get()
                .getEventBus()
                .dispatchForAsync(RemoveCheatingPlayerEvent.class)
                .dispatch(new RemoveCheatingPlayerEvent(ref, reason));
    }

    // TODO: probably want to make this return an enum of what check failed for downstream or move checks to some sort of abstract interface
    private static boolean checkInvalidAbsoluteMovementPacket(PlayerInput.AbsoluteMovement absoluteMovement, TransformComponent transformComponent, MovementStatesComponent movementStatesComponent, float deltaTime) {
        var movementStates = movementStatesComponent.getMovementStates();

        var newPosition = new Vector3d(absoluteMovement.getX(), absoluteMovement.getY(), absoluteMovement.getZ());
        var oldPosition = transformComponent.getPosition();
        var movementDelta = newPosition.subtract(oldPosition);
        var movementDistance = movementDelta.length();
        var movementSpeed = movementDistance / deltaTime;

        var newLateralPosition = new Vector3d(absoluteMovement.getX(), absoluteMovement.getY(), absoluteMovement.getZ());
        newLateralPosition.y = 0;
        var oldLateralPosition = transformComponent.getPosition().clone();
        oldLateralPosition.y = 0;
        var lateralMovementDelta = newLateralPosition.subtract(oldLateralPosition);
        var lateralMovementDistance = lateralMovementDelta.length();
        var lateralMovementSpeed = lateralMovementDistance / deltaTime;

        var verticalMovementSpeed = movementSpeed - lateralMovementSpeed;



        if (lateralMovementSpeed > 15.0) {
            // failed lateral speed check

            return true;
        }
        if (verticalMovementSpeed > 60.0 && !movementStates.falling) {
            // failed vertical speed check
            return true;
        }
        return false;
    }

    private static void cancelInputUpdate(PlayerInput.InputUpdate inputUpdate,
                                          List<PlayerInput.InputUpdate> removalQueue,
                                          @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                                          @Nonnull Store<EntityStore> store,
                                          @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                          int index) {
        var transformComponent = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        var headRotationComponent = archetypeChunk.getComponent(index, HeadRotation.getComponentType());

        removalQueue.add(inputUpdate);

        var playerRef = archetypeChunk.getReferenceTo(index);
        var position = transformComponent.getPosition();
        var rotation = transformComponent.getRotation();
        var headRotation = headRotationComponent.getRotation();

        var playerRefComponent = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        var teleportPacket = new ClientTeleport(
                (byte)0,
                new ModelTransform(
                        PositionUtil.toPositionPacket(position),
                        PositionUtil.toDirectionPacket(rotation),
                        PositionUtil.toDirectionPacket(headRotation)
                ),
                false
        );
        var statePacket = new SetMovementStates(new SavedMovementStates(false));

        playerRefComponent.getPacketHandler().write(teleportPacket);
        playerRefComponent.getPacketHandler().write(statePacket);


//        var teleport = new Teleport(position, rotation).withHeadRotation(headRotation);
//        store.addComponent(playerRef, Teleport.getComponentType(), teleport);

        /*
        switch (inputUpdate) {
            case PlayerInput.AbsoluteMovement absoluteMovement:
                playerComponent.moveTo(playerRef, position.x, position.y, position.z, commandBuffer);
                break;
            case PlayerInput.RelativeMovement relativeMovement:
                playerComponent.moveTo(playerRef, position.x, position.y, position.z, commandBuffer);
                break;
            case PlayerInput.SetBody body:
                rotation.assign(rotation);
                break;
            case PlayerInput.SetClientVelocity clientVelocity:
                velocityComponent.setClient(velocityComponent.getVelocity());
                break;
            case PlayerInput.SetHead head:
                headRotation.assign(rotation.x, rotation.y, rotation.z);
                break;
            case PlayerInput.SetMovementStates movementStates:
                // TODO: this may not update client of new movement states
                movementStatesComponent.setMovementStates(movementStates.movementStates());
                break;
            case PlayerInput.SetRiderMovementStates riderMovementStates:
                // does nothing in Hytale natively
                break;
            case PlayerInput.WishMovement wishMovement:
                // does nothing in Hytale natively
                break;
            default:
                break;
        }
         */
    }
}
