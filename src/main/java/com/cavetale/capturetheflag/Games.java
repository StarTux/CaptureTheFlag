package com.cavetale.capturetheflag;

import com.cavetale.core.util.Json;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import static com.cavetale.capturetheflag.CaptureTheFlagPlugin.plugin;

public final class Games {
    // Map actual loaded world name, NOT the map name
    private final Map<String, Game> gameMap = new HashMap<>();
    private File saveFile;
    private Save save;
    private List<String> mapNames = new ArrayList<>();

    public void enable() {
        loadConfig();
        CaptureTheFlagPlugin.plugin().getDataFolder().mkdirs();
        saveFile = new File(CaptureTheFlagPlugin.plugin().getDataFolder(), "save.json");
        load();
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
}
