package dev.solarion.anticheat.event;

import com.hypixel.hytale.event.IAsyncEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public record RemoveCheatingPlayerEvent(@Nonnull PlayerRef player,
                                        @Nonnull String reason) implements IAsyncEvent<String> {
}
