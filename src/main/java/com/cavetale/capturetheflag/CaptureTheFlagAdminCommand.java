package com.cavetale.capturetheflag;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public final class CaptureTheFlagAdminCommand extends AbstractCommand<CaptureTheFlagPlugin> {
    protected CaptureTheFlagAdminCommand(final CaptureTheFlagPlugin plugin) {
        super(plugin, "capturetheflagadmin");
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
