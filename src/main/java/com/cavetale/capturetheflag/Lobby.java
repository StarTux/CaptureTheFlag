package com.cavetale.capturetheflag;

import com.cavetale.core.event.minigame.MinigameMatchType;
import com.winthier.creative.vote.MapVote;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import static com.cavetale.afk.AFKPlugin.isAfk;

@RequiredArgsConstructor
public final class Lobby {
    private final CaptureTheFlagPlugin plugin;
    private LobbyListener listener;

    public void enable() {
        listener = new LobbyListener(plugin, this);
        listener.enable();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public World getWorld() {
        return Bukkit.getWorlds().get(0);
    }

    public boolean isWorld(World world) {
        return world.equals(getWorld());
    }

    public List<Player> getPlayers() {
        return getWorld().getPlayers();
    }

    public Location getSpawnLocation() {
        return getWorld().getSpawnLocation();
    }

    public void onJoin(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setVelocity(new Vector());
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
    }

    private void tick() {
        if (plugin.getGames().getSave().isPause()) {
            MapVote.stop(MinigameMatchType.CAPTURE_THE_FLAG);
            return;
        }
        if (plugin.getGames().getSave().isEvent() && !plugin.getGames().isEmpty()) {
            MapVote.stop(MinigameMatchType.CAPTURE_THE_FLAG);
            return;
        }
        int availablePlayerCount = 0;
        for (Player player : getPlayers()) {
            if (!isAfk(player)) {
                availablePlayerCount += 1;
            }
        }
        if (availablePlayerCount >= 2 && !MapVote.isActive(MinigameMatchType.CAPTURE_THE_FLAG)) {
            MapVote.start(MinigameMatchType.CAPTURE_THE_FLAG, v -> {
                    v.setTitle(Games.TITLE);
                    v.setLobbyWorld(getWorld());
                    v.setCallback(result -> {
                            plugin.getGames().startGame(result.getBuildWorldWinner(), result.getLocalWorldCopy());
                        });
                    v.setAvoidRepetition(1);
                });
        } else if (availablePlayerCount < 2) {
            MapVote.stop(MinigameMatchType.CAPTURE_THE_FLAG);
        }
    }
}
