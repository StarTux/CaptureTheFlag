package com.cavetale.capturetheflag;

import com.cavetale.core.event.player.PlayerTeamQuery;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import static net.kyori.adventure.text.Component.text;

@Data
public final class GameTeam {
    private final Game game;
    private final Team team;
    private int score;
    private PlayerTeamQuery.Team coreTeam;
    private final Set<UUID> members = new HashSet<>();
    private final List<Cuboid> spawns = new ArrayList<>();
    private Vec3i flagSpawn;
    private GameFlag gameFlag;

    public GameTeam(final Game game, final Team team) {
        this.game = game;
        this.team = team;
        this.coreTeam = new PlayerTeamQuery.Team(game.getLoadedWorldName() + "_" + team.key,
                                                 text(team.displayName, team.textColor),
                                                 team.textColor);
    }

    public void disable() {
        if (gameFlag != null) {
            gameFlag.disable();
            gameFlag = null;
        }
    }

    public boolean isFlagHome() {
        return gameFlag == null || gameFlag.getVector().equals(flagSpawn);
    }

    public void resetFlag() {
        if (gameFlag == null) return;
        gameFlag.disable();
        gameFlag = null;
    }

    public int getMemberCount() {
        return members.size();
    }
}
