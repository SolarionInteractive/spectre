package dev.solarion.anticheat;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.solarion.anticheat.events.KickPlayerEvent;
import dev.solarion.anticheat.events.RemoveCheatingPlayerEvent;
import dev.solarion.anticheat.systems.AnticheatSystem;

import javax.annotation.Nonnull;

public class AnticheatPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AnticheatPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Anticheat");

        this.getEventRegistry().registerGlobal(RemoveCheatingPlayerEvent.class, KickPlayerEvent::onRemoveCheatingPlayerEvent);
        this.getEntityStoreRegistry().registerSystem(new AnticheatSystem.ScanPlayerInput());
    }
}
