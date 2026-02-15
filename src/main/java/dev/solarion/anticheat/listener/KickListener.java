package dev.solarion.anticheat.listener;

import com.hypixel.hytale.server.core.HytaleServer;
import dev.solarion.anticheat.check.CheckManager;
import dev.solarion.anticheat.event.RemoveCheatingPlayerEvent;

import java.util.Objects;

public class KickListener {

    private final CheckManager checkManager;

    public KickListener(CheckManager checkManager) {
        this.checkManager = checkManager;
    }

    public void onRemoveCheatingPlayerEvent(RemoveCheatingPlayerEvent event) {
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            if (!checkManager.isDisconnecting(event.player().getUuid()) && Objects.requireNonNull(event.player().getReference()).isValid()) {
                event.player().getPacketHandler().disconnect(event.reason());
            }
        });
    }
}
