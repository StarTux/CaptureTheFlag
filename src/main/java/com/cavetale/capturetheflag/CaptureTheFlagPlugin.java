package com.cavetale.capturetheflag;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class CaptureTheFlagPlugin extends JavaPlugin {
    private static CaptureTheFlagPlugin instance;
    private final CaptureTheFlagCommand captureTheFlagCommand = new CaptureTheFlagCommand(this);
    private final CaptureTheFlagAdminCommand captureTheFlagAdminCommand = new CaptureTheFlagAdminCommand(this);
    private final GameListener gameListener = new GameListener(this);
    private final Games games = new Games(this);
    private final Items items = new Items();
    private final Lobby lobby = new Lobby(this);

    public CaptureTheFlagPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        captureTheFlagCommand.enable();
        captureTheFlagAdminCommand.enable();
        gameListener.enable();
        games.enable();
        items.enable();
        lobby.enable();
    }

    @Override
    public void onDisable() {
        games.disable();
    }

    public static CaptureTheFlagPlugin plugin() {
        return instance;
    }
}
