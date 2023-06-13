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
    private Team team;

    public GamePlayer(final Player player) {
        this(player.getUniqueId(), player.getName());
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}
