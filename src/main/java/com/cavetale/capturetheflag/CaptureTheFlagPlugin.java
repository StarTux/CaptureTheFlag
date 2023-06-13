package com.cavetale.capturetheflag;

import org.bukkit.plugin.java.JavaPlugin;

public final class CaptureTheFlagPlugin extends JavaPlugin {
    protected static CaptureTheFlagPlugin instance;
    protected final CaptureTheFlagCommand captureTheFlagCommand = new CaptureTheFlagCommand(this);
    protected final CaptureTheFlagAdminCommand captureTheFlagAdminCommand = new CaptureTheFlagAdminCommand(this);
    protected final EventListener eventListener = new EventListener();
    protected final Games games = new Games();

    public CaptureTheFlagPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        captureTheFlagCommand.enable();
        captureTheFlagAdminCommand.enable();
        eventListener.enable();
        games.enable();
    }

    @Override
    public void onDisable() {
        games.disable();
    }

    public static CaptureTheFlagPlugin plugin() {
        return instance;
    }
}
