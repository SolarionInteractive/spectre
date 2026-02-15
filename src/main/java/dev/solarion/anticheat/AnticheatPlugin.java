package dev.solarion.anticheat;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import dev.solarion.anticheat.check.CheckManager;
import dev.solarion.anticheat.event.RemoveCheatingPlayerEvent;
import dev.solarion.anticheat.listener.KickListener;
import dev.solarion.anticheat.listener.PlayerListener;
import dev.solarion.anticheat.system.ACInputSystem;

import javax.annotation.Nonnull;

public class AnticheatPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AnticheatPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Anticheat");

        CheckManager checkManager = new CheckManager();

        this.getEventRegistry().registerGlobal(RemoveCheatingPlayerEvent.class, new KickListener(checkManager)::onRemoveCheatingPlayerEvent);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, new PlayerListener(checkManager)::onPlayerDisconnect);
        this.getEventRegistry().registerGlobal(com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent.class, new PlayerListener(checkManager)::onPlayerConnect);
        
        this.getEntityStoreRegistry().registerSystem(new ACInputSystem(checkManager));
    }
}
