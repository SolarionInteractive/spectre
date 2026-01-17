package dev.solarion.anticheat.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerEvent;

public class KickPlayerEvent
{
    public static void onRemoveCheatingPlayerEvent(RemoveCheatingPlayerEvent event) {
        event.player.getPacketHandler().disconnect(event.reason);
    }
}
