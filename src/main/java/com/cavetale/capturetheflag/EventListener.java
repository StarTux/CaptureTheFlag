package com.cavetale.capturetheflag;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.player.PlayerTeamQuery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class EventListener implements Listener {
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, CaptureTheFlagPlugin.plugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        Game game = Game.of(event.getPlayer());
        if (game != null) game.onPlayerHud(event);
    }

    private void onPlayerDeath(PlayerDeathEvent event) {
        Game game = Game.of(event.getPlayer());
        if (game != null) game.onPlayerDeath(event);
    }

    private void onEntityDeath(EntityDeathEvent event) {
        Game game = Game.of(event.getEntity());
        if (game != null) game.onEntityDeath(event);
    }

    private void onEntityDamage(EntityDamageEvent event) {
        Game game = Game.of(event.getEntity());
        if (game != null) game.onEntityDamage(event);
    }

    @EventHandler
    private void onPlayerTeam(PlayerTeamQuery query) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Game game = Game.of(player);
            if (game == null) continue;
            GameTeam team = game.getTeam(player);
            if (team == null) continue;
            query.setTeam(player, team.getCoreTeam());
        }
    }
}
