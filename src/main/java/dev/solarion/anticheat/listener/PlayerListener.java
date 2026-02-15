package dev.solarion.anticheat.listener;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.solarion.anticheat.check.CheckManager;

import java.util.UUID;

public class PlayerListener {

    private final CheckManager checkManager;

    public PlayerListener(CheckManager checkManager) {
        this.checkManager = checkManager;
    }

    public void onPlayerConnect(PlayerConnectEvent event) {
        UUID playerId = event.getPlayerRef().getUuid();
        checkManager.onPlayerConnect(playerId);
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        UUID playerId = playerRef.getUuid();
        checkManager.markDisconnecting(playerId);
        checkManager.onPlayerLogout(playerId);
    }
}
