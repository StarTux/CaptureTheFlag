package com.cavetale.capturetheflag;

import com.cavetale.core.util.Json;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import lombok.Data;
import net.kyori.adventure.text.Component;
import static com.cavetale.capturetheflag.CaptureTheFlagPlugin.plugin;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Data
public final class Games {
    private static Games instance;
    // Map actual loaded world name, NOT the map name
    private final Map<String, Game> gameMap = new HashMap<>();
    private File saveFile;
    private Save save;
    private List<String> mapNames = new ArrayList<>();
    public static final Component TITLE = textOfChildren(text("Capture", RED),
                                                         text(tiny("the"), GRAY),
                                                         text("Flag", BLUE));
    private List<Highscore> highscores = List.of();
    private List<Component> highscoreLines = List.of();

    protected Games() {
        instance = this;
    }

    public static Games games() {
        return instance;
    }

    public void enable() {
        loadConfig();
        CaptureTheFlagPlugin.plugin().getDataFolder().mkdirs();
        saveFile = new File(CaptureTheFlagPlugin.plugin().getDataFolder(), "save.json");
        load();
        computeHighscores();
    }

    public void disable() {
        for (Game game : gameMap.values()) {
            game.disable();
        }
        gameMap.clear();
    }

    public void loadConfig() {
        mapNames.addAll(plugin().getConfig().getStringList("Maps"));
    }

    public void load() {
        this.save = Json.load(saveFile, Save.class, Save::new);
    }

    public void save() {
        Json.save(saveFile, save, true);
    }

    public boolean startGame(String mapName) {
        Game game = new Game(mapName);
        try {
            game.enable();
        } catch (Exception e) {
            plugin().getLogger().log(Level.SEVERE, "Load game " + mapName, e);
            try {
                game.disable();
            } catch (Exception f) {
                plugin().getLogger().log(Level.SEVERE, "Disable game " + mapName, e);
            }
            return false;
        }
        gameMap.put(game.getLoadedWorldName(), game);
        return true;
    }

    public void computeHighscores() {
        highscores = Highscore.of(save.getScores());
        highscoreLines = Highscore.sidebar(highscores, TrophyCategory.SWORD);
    }

    public int rewardScores() {
        return Highscore.reward(save.getScores(),
                                "capture_the_flag",
                                TrophyCategory.SWORD,
                                TITLE,
                                hi -> "You earned a score of " + hi.score);
    }

    public void disable(Game game) {
        Game game2 = gameMap.remove(game.getLoadedWorldName());
        game.disable();
    }
}
