package com.cavetale.capturetheflag;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public final class CaptureTheFlagCommand extends AbstractCommand<CaptureTheFlagPlugin> {
    protected CaptureTheFlagCommand(final CaptureTheFlagPlugin plugin) {
        super(plugin, "capturetheflag");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Info Command")
            .senderCaller(this::info);
    }

    protected boolean info(CommandSender sender, String[] args) {
        return false;
    }
}
