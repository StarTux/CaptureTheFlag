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
    private int totalDeathTicks = Game.INIT_DEATH_TICKS;
    private int money;

    public GamePlayer(final Player player) {
        this(player.getUniqueId(), player.getName());
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public Team getTeam() {
        return gameTeam.getTeam();
    }

    public void addMoney(int amount) {
        money += amount;
    }
}
