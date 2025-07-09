package fr.roguire.dynamicwaterfall.listeners;

import fr.roguire.dynamicwaterfall.DynamicWaterfall;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class OnPlayerJoinEvent implements Listener {

    private final DynamicWaterfall
        plugin;

    public OnPlayerJoinEvent(DynamicWaterfall plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerKick(ServerKickEvent event) {
        if (event.getReason().toLegacyText().contains("closed")) {
            ServerInfo lobby = plugin.getProxy().getServerInfo("lobby");
            if (lobby != null) {
                event.setCancelled(true);
                event.setCancelServer(lobby);
            }
        }
    }


}
