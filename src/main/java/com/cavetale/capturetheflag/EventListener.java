package com.cavetale.capturetheflag;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.player.PlayerTPAEvent;
import com.cavetale.core.event.player.PlayerTeamQuery;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import static com.cavetale.capturetheflag.Games.games;

public final class EventListener implements Listener {
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, CaptureTheFlagPlugin.plugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        Game game = Game.of(event.getPlayer());
        if (game != null) game.onPlayerJoin(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        Game game = Game.of(event.getPlayer());
        if (game != null) {
            game.onPlayerHud(event);
        } else {
            List<Component> sidebar = new ArrayList<>();
            sidebar.add(Games.TITLE);
            if (games().getSave().isEvent()) {
                sidebar.addAll(games().getHighscoreLines());
            }
            event.sidebar(PlayerHudPriority.HIGH, sidebar);
        }
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        Game game = Game.of(event.getPlayer());
        if (game != null) game.onPlayerDeath(event);
    }

    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Game game = Game.of(event.getPlayer());
        if (game != null) game.onPlayerInteractEntity(event);
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Game game = Game.of(event.getEntity());
        if (game != null) game.onEntityDamageByEntity(event);
    }

    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        Game game = Game.of(event.getPlayer());
        if (game != null) game.onPlayerRespawn(event);
    }

    @EventHandler
    private void onEntityDeath(EntityDeathEvent event) {
        Game game = Game.of(event.getEntity());
        if (game != null) game.onEntityDeath(event);
    }

    @EventHandler
    private void onEntityDamage(EntityDamageEvent event) {
        Game game = Game.of(event.getEntity());
        if (game != null) game.onEntityDamage(event);
    }

    @EventHandler
    private void onPlayerTPA(PlayerTPAEvent event) {
        Game game = Game.of(event.getTarget());
        if (game != null) event.setCancelled(true);
    }

    @EventHandler
    private void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Game game = Game.of(event.getPlayer());
        if (game != null) event.setCancelled(true);
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

    @EventHandler
    private void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        Location old = event.getSpawnLocation();
        if (old.getWorld().equals(Bukkit.getWorlds().get(0))) {
            event.setSpawnLocation(old.getWorld().getSpawnLocation());
            for (Game game : games().getGameMap().values()) {
                event.setSpawnLocation(game.getWorld().getSpawnLocation());
            }
        }
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        onBuild(event, event.getPlayer(), event.getBlock());
        Game.applyGameIn(event.getBlock().getWorld(), game -> game.onBlockBreak(event));
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        onBuild(event, event.getPlayer(), event.getBlock());
        Game.applyGameIn(event.getBlock().getWorld(), game -> game.onBlockPlace(event));
    }

    private void onBuild(Cancellable event, Player player, Block block) {
        Game game = Game.in(block.getWorld());
        if (game != null) game.onBuild(event, player, block);
    }

    @EventHandler
    private void onBlockExplode(BlockExplodeEvent event) {
        onExplode(event, event.getBlock().getWorld(), event.blockList());
    }

    @EventHandler
    private void onEntityExplode(EntityExplodeEvent event) {
        onExplode(event, event.getEntity().getWorld(), event.blockList());
    }

    private void onExplode(Cancellable event, World world, List<Block> blockList) {
        Game game = Game.in(world);
        if (game != null) game.onExplode(event, blockList);
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Game.applyGameIn(event.getPlayer().getWorld(), game -> game.onPlayerInteract(event));
    }

    @EventHandler
    private void onEntityTarget(EntityTargetEvent event) {
        Game.applyGameIn(event.getEntity().getWorld(), game -> game.onEntityTarget(event));
    }
}
