package dev.solarion.anticheat.events;

import com.hypixel.hytale.event.IAsyncEvent;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class RemoveCheatingPlayerEvent implements IAsyncEvent<String> {
    @Nonnull
    public final PlayerRef player;
    @Nonnull
    public final String reason;

    public RemoveCheatingPlayerEvent(@Nonnull PlayerRef player, @Nonnull String reason) {
        this.player = player;
        this.reason = reason;
    }
}
