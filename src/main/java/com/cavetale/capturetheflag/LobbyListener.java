package com.cavetale.capturetheflag;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
public final class LobbyListener implements Listener {
    private final CaptureTheFlagPlugin plugin;
    private final Lobby lobby;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        if (!lobby.isWorld(event.getPlayer().getWorld())) return;
        if (!plugin.getGames().getSave().isEvent()) return;
        final List<Component> sidebar = new ArrayList<>();
        sidebar.add(Games.TITLE);
        sidebar.addAll(plugin.getGames().getHighscoreLines());
        event.sidebar(PlayerHudPriority.HIGH, sidebar);
    }

    /**
     * Force lobby spawn.
     */
    @EventHandler(priority = EventPriority.LOW)
    private void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        if (!lobby.isWorld(event.getSpawnLocation().getWorld())) return;
        event.setSpawnLocation(lobby.getSpawnLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!lobby.isWorld(event.getPlayer().getWorld())) return;
        lobby.onJoin(event.getPlayer());
    }

    @EventHandler
    private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!lobby.isWorld(event.getPlayer().getWorld())) return;
        lobby.onJoin(event.getPlayer());
    }
}
