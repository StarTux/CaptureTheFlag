package com.cavetale.capturetheflag;

import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Data @RequiredArgsConstructor
public final class GamePlayer {
    private final UUID uuid;
    private final String name;
    private GameTeam gameTeam;
    private boolean dead;
    private int kills;
    private int deaths;
    private int deathTicks;

    public GamePlayer(final Player player) {
        this(player.getUniqueId(), player.getName());
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public Team getTeam() {
        return gameTeam.getTeam();
    }
}
