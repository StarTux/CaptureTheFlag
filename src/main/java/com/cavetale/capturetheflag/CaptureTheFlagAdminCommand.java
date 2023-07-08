package com.cavetale.capturetheflag;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
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
        rootNode.addChild("event").arguments("[true|false]")
            .completers(CommandArgCompleter.BOOLEAN)
            .description("Set event mode")
            .senderCaller(this::event);
        // Score
        CommandNode scoreNode = rootNode.addChild("score")
            .description("Score commands");
        scoreNode.addChild("add")
            .description("Manipulate score")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i != 0))
            .senderCaller(this::scoreAdd);
        scoreNode.addChild("clear").denyTabCompletion()
            .description("Clear all scores")
            .senderCaller(this::scoreClear);
        scoreNode.addChild("reward").denyTabCompletion()
            .description("Reward players")
            .senderCaller(this::scoreReward);
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String mapName = args[0];
        Games.games().startGame(mapName);
        return true;
    }

    private void stop(CommandSender sender) {
        if (sender instanceof Player player) {
            Game game = Game.of(player);
            if (game == null) throw new CommandWarn("No game!");
            games().disable(game);
            player.sendMessage(text("Game stopped: " + game.getLoadedWorldName(), YELLOW));
        } else {
            int count = 0;
            for (Game game : List.copyOf(games().getGameMap().values())) {
                games().disable(game);
                count += 1;
            }
            sender.sendMessage(text("Stopped " + count + " game(s)", YELLOW));
        }
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

    private boolean event(CommandSender sender, String[] args) {
        if (args.length == 1) {
            boolean value = CommandArgCompleter.requireBoolean(args[0]);
            games().getSave().setEvent(value);
            games().save();
            sender.sendMessage(text("Event mode: " + value, value ? AQUA : RED));
            return true;
        } else if (args.length == 0) {
            boolean value = games().getSave().isEvent();
            sender.sendMessage(text("Event mode: " + value, value ? AQUA : RED));
            return true;
        } else {
            return false;
        }
    }

    private boolean scoreClear(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        games().getSave().getScores().clear();
        games().save();
        games().computeHighscores();
        sender.sendMessage(text("All scores cleared", AQUA));
        return true;
    }

    private boolean scoreAdd(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        int value = CommandArgCompleter.requireInt(args[1], i -> i != 0);
        games().getSave().addScore(target.uuid, value);
        games().save();
        games().computeHighscores();
        sender.sendMessage(text("Score of " + target.name + " manipulated by " + value, AQUA));
        return true;
    }

    private void scoreReward(CommandSender sender) {
        int count = games().rewardScores();
        sender.sendMessage(text("Rewarded " + count + " players", AQUA));
    }
}
