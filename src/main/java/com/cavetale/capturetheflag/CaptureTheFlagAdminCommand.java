package com.cavetale.capturetheflag;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.mytems.util.Gui;
import com.winthier.creative.BuildWorld;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.capturetheflag.Games.games;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class CaptureTheFlagAdminCommand extends AbstractCommand<CaptureTheFlagPlugin> {
    protected CaptureTheFlagAdminCommand(final CaptureTheFlagPlugin plugin) {
        super(plugin, "capturetheflagadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("start").arguments("<map>")
            .description("Start the game")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(Games.games().getMaps().keySet())))
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
        // Recipe
        CommandNode recipeNode = rootNode.addChild("recipe")
            .description("Recipe commands");
        recipeNode.addChild("list").arguments("<type>")
            .description("List recipes")
            .completers(CommandArgCompleter.enumLowerList(RecipeType.class))
            .senderCaller(this::recipeList);
        recipeNode.addChild("add").arguments("<type>")
            .description("Add recipe")
            .completers(CommandArgCompleter.enumLowerList(RecipeType.class))
            .playerCaller(this::recipeAdd);
        recipeNode.addChild("edit").arguments("<type> <index>")
            .description("Edit recipe")
            .completers(CommandArgCompleter.enumLowerList(RecipeType.class),
                        CommandArgCompleter.integer(i -> i >= 0))
            .playerCaller(this::recipeEdit);
        recipeNode.addChild("remove").arguments("<type> <index>")
            .description("Remove recipe")
            .completers(CommandArgCompleter.enumLowerList(RecipeType.class),
                        CommandArgCompleter.integer(i -> i >= 0))
            .senderCaller(this::recipeRemove);
        // Team
        CommandNode teamNode = rootNode.addChild("team")
            .description("Team commands");
        teamNode.addChild("addscore").arguments("<team> <amount>")
            .completers(CommandArgCompleter.enumLowerList(Team.class),
                        CommandArgCompleter.integer(i -> i != 0))
            .description("Change team score")
            .senderCaller(this::teamAddScore);
        // Item
        CommandNode itemNode = rootNode.addChild("item")
            .description("Item commands");
        itemNode.addChild("landmine").denyTabCompletion()
            .description("Spawn land mine")
            .playerCaller(p -> p.getInventory().addItem(Items.items().landMine));
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        BuildWorld buildWorld = Games.games().getMaps().get(args[0]);
        if (buildWorld == null) throw new CommandWarn("Map not found: " + args[0]);
        Games.games().startGame(buildWorld);
        sender.sendMessage(text("Game starting: " + buildWorld.getName(), YELLOW));
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
        case PLAY:
            game.setStateTicks(game.getStateTicks() + 20 * 60);
            player.sendMessage(text("Time progressed", YELLOW));
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

    private boolean recipeList(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        RecipeType type = CommandArgCompleter.requireEnum(RecipeType.class, args[0]);
        int nextIndex = 0;
        for (Recipe recipe : games().getRecipeSave().get(type)) {
            int index = nextIndex++;
            List<ItemStack> items = recipe.getItems();
            ItemStack a = items.get(0);
            ItemStack b = items.get(1);
            ItemStack c = items.get(2);
            sender.sendMessage(textOfChildren(text(index + ") ", YELLOW),
                                              (a != null ? ItemKinds.chatDescription(a) : text("-", DARK_RED)),
                                              text(" + ", GRAY),
                                              (b != null ? ItemKinds.chatDescription(b) : text("-", DARK_RED)),
                                              text(" -> ", GRAY),
                                              (c != null ? ItemKinds.chatDescription(c) : text("-", DARK_RED))));
        }
        sender.sendMessage(text(nextIndex + " total recipes: " + type, YELLOW));
        return true;
    }

    private boolean recipeAdd(Player player, String[] args) {
        if (args.length != 1) return false;
        final RecipeType type = CommandArgCompleter.requireEnum(RecipeType.class, args[0]);
        final Gui gui = new Gui(plugin).rows(1).title(text("Add recipe " + type));
        gui.setEditable(true);
        gui.onClose(close -> {
                ItemStack a = gui.getInventory().getItem(0);
                ItemStack b = gui.getInventory().getItem(1);
                ItemStack c = gui.getInventory().getItem(2);
                if (a == null || c == null) {
                    player.sendMessage(text("No recipe created", RED));
                    return;
                }
                Recipe recipe = new Recipe();
                recipe.setItems(a, b, c);
                games().getRecipeSave().get(type).add(recipe);
                games().saveRecipes();
                player.sendMessage(textOfChildren(text("Recipe created: ", YELLOW),
                                                  (a != null ? ItemKinds.chatDescription(a) : text("-", DARK_RED)),
                                                  text(" + ", GRAY),
                                                  (b != null ? ItemKinds.chatDescription(b) : text("-", DARK_RED)),
                                                  text(" -> ", GRAY),
                                                  (c != null ? ItemKinds.chatDescription(c) : text("-", DARK_RED))));
            });
        gui.open(player);
        return true;
    }

    private boolean recipeEdit(Player player, String[] args) {
        if (args.length != 2) return false;
        final RecipeType type = CommandArgCompleter.requireEnum(RecipeType.class, args[0]);
        final int index = CommandArgCompleter.requireInt(args[1], i -> i >= 0);
        if (index >= games().getRecipeSave().get(type).size()) {
            throw new CommandWarn("Index out of bounds: " + index);
        }
        final Recipe recipe = games().getRecipeSave().get(type).get(index);
        List<ItemStack> items = recipe.getItems();
        final Gui gui = new Gui(plugin).rows(1).title(text("Add recipe " + type));
        gui.setEditable(true);
        for (int i = 0; i < 3; i += 1) {
            gui.setItem(i, items.get(i));
        }
        gui.onClose(close -> {
                ItemStack a = gui.getInventory().getItem(0);
                ItemStack b = gui.getInventory().getItem(1);
                ItemStack c = gui.getInventory().getItem(2);
                if (a == null || c == null) {
                    player.sendMessage(text("Recipe not edited", RED));
                    return;
                }
                recipe.setItems(a, b, c);
                games().saveRecipes();
                player.sendMessage(textOfChildren(text("Recipe edited: ", YELLOW),
                                                  text(index + ") ", YELLOW),
                                                  (a != null ? ItemKinds.chatDescription(a) : text("-", DARK_RED)),
                                                  text(" + ", GRAY),
                                                  (b != null ? ItemKinds.chatDescription(b) : text("-", DARK_RED)),
                                                  text(" -> ", GRAY),
                                                  (c != null ? ItemKinds.chatDescription(c) : text("-", DARK_RED))));
            });
        gui.open(player);
        return true;
    }

    private boolean recipeRemove(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final RecipeType type = CommandArgCompleter.requireEnum(RecipeType.class, args[0]);
        final int index = CommandArgCompleter.requireInt(args[1], i -> i >= 0);
        if (index >= games().getRecipeSave().get(type).size()) {
            throw new CommandWarn("Index out of bounds: " + index);
        }
        final Recipe recipe = games().getRecipeSave().get(type).remove(index);
        games().saveRecipes();
        List<ItemStack> items = recipe.getItems();
        final ItemStack a = items.get(0);
        final ItemStack b = items.get(1);
        final ItemStack c = items.get(2);
        sender.sendMessage(textOfChildren(text("Recipe removed: ", YELLOW),
                                          text(index + ") ", YELLOW),
                                          (a != null ? ItemKinds.chatDescription(a) : text("-", DARK_RED)),
                                          text(" + ", GRAY),
                                          (b != null ? ItemKinds.chatDescription(b) : text("-", DARK_RED)),
                                          text(" -> ", GRAY),
                                          (c != null ? ItemKinds.chatDescription(c) : text("-", DARK_RED))));
        return true;
    }

    private boolean teamAddScore(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        Team team = CommandArgCompleter.requireEnum(Team.class, args[0]);
        int value = CommandArgCompleter.requireInt(args[1], i -> i != 0);
        if (sender instanceof Player player) {
            Game game = Game.of(player);
            if (game == null) throw new CommandWarn("No game!");
            GameTeam gameTeam = game.getTeamMap().get(team);
            gameTeam.setScore(gameTeam.getScore() + value);
            player.sendMessage(text(team + " score changed to " + gameTeam.getScore(), YELLOW));
        } else {
            for (Game game : List.copyOf(games().getGameMap().values())) {
                GameTeam gameTeam = game.getTeamMap().get(team);
                gameTeam.setScore(gameTeam.getScore() + value);
                sender.sendMessage(text(team + " score changed to " + gameTeam.getScore(), YELLOW));
            }
        }
        return true;
    }
}
