package com.cavetale.capturetheflag;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static com.cavetale.capturetheflag.Games.games;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class CaptureTheFlagAdminCommand extends AbstractCommand<CaptureTheFlagPlugin> {
    protected CaptureTheFlagAdminCommand(final CaptureTheFlagPlugin plugin) {
        super(plugin, "capturetheflagadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("start").arguments("<map>")
            .description("Start the game")
            .completers(CommandArgCompleter.supplyList(() -> Games.games().getMapNames()))
            .senderCaller(this::start);
        rootNode.addChild("stop").denyTabCompletion()
            .description("Stop the game")
            .senderCaller(this::stop);
        rootNode.addChild("skip").denyTabCompletion()
            .description("Skip timer")
            .playerCaller(this::skip);
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String mapName = args[0];
        Games.games().startGame(mapName);
        return true;
    }

    private void stop(CommandSender sender) {
        int count = 0;
        for (Game game : List.copyOf(games().getGameMap().values())) {
            game.disable();
            count += 1;
        }
        games().getGameMap().clear();
        sender.sendMessage(text("Stopped " + count + " game(s)", YELLOW));
    }

    private void skip(Player player) {
        Game game = Game.of(player);
        if (game == null) throw new CommandWarn("No game!");
        switch (game.getState()) {
        case COUNTDOWN:
            game.setStateTicks(20 * 60);
            player.sendMessage(text("Countdown skipped", YELLOW));
            break;
        default: throw new CommandWarn("Cannot skip!");
        }
    }
}
