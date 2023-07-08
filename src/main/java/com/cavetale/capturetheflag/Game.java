package com.cavetale.capturetheflag;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.capturetheflag.world.Worlds;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import static com.cavetale.capturetheflag.CaptureTheFlagPlugin.plugin;
import static com.cavetale.capturetheflag.Games.games;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.title.Title.title;

@Data
public final class Game {
    private final String mapName;
    private String loadedWorldName; // loaded world name
    private World world;
    private List<Cuboid> gameAreas = new ArrayList<>();
    private List<Cuboid> creepAreas = new ArrayList<>();
    private Map<Vec3i, String> merchantBlocks = new HashMap<>();
    private BukkitTask task;
    private Map<UUID, GamePlayer> playerMap = new HashMap<>();
    private Map<Team, GameTeam> teamMap = new EnumMap<>(Team.class);
    private Random random = new Random();
    private GameState state = GameState.INIT;
    private int ticks;
    private int stateTicks;
    private Component timeComponent = empty();
    private BossBar bossbar = BossBar.bossBar(Games.TITLE, 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
    private static final int GAME_TIME_TICKS = 20 * 20 * 60;

    public Game(final String mapName) {
        this.mapName = mapName;
    }

    public static Game in(World world) {
        return Games.games().getGameMap().get(world.getName());
    }

    public static Game of(Entity entity) {
        return in(entity.getWorld());
    }

    public void enable() {
        loadWorld();
        if (world == null) {
            throw new IllegalStateException("World didn't load: " + mapName);
        }
        for (Team team : Team.values()) {
            teamMap.put(team, new GameTeam(this, team));
        }
        loadAreas();
        loadChunks();
        makeTeams();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin(), this::tick, 1L, 1L);
        this.state = GameState.COUNTDOWN;
    }

    public void disable() {
        state = GameState.INIT;
        for (GameTeam gameTeam : teamMap.values()) {
            gameTeam.disable();
        }
        for (Player player : world.getPlayers()) {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setFireTicks(0);
            player.setFallDistance(0f);
        }
        if (world != null) {
            world.removePluginChunkTickets(plugin());
            Worlds.deleteWorld(world);
            world = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void announce(Component text) {
        for (Player player : world.getPlayers()) {
            player.sendMessage(text);
        }
    }

    public void announceTitle(Component title, Component subtitle) {
        for (Player player : world.getPlayers()) {
            player.showTitle(title(title, subtitle));
        }
    }

    private void loadWorld() {
        this.world = Worlds.loadWorldCopy(mapName);
        if (world == null) throw new IllegalStateException("Loading world " + mapName);
        this.loadedWorldName = world.getName();
        world.setGameRule(GameRule.NATURAL_REGENERATION, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, true);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.MOB_GRIEFING, true);
        world.setDifficulty(Difficulty.PEACEFUL);
    }

    private void loadAreas() {
        AreasFile areasFile = AreasFile.load(world, "CaptureTheFlag");
        if (areasFile == null) {
            throw new IllegalStateException("No areas file: " + mapName);
        }
        for (Area area : areasFile.find("game")) {
            gameAreas.add(area.toCuboid());
        }
        for (Area area : areasFile.find("creep")) {
            creepAreas.add(area.toCuboid());
        }
        for (Area area : areasFile.find("spawn")) {
            Team team = Team.ofKey(area.getName());
            if (team == null) {
                plugin().getLogger().severe("Invalid spawn: " + area);
                continue;
            }
            teamMap.get(team).getSpawns().add(area.toCuboid());
        }
        for (Area area : areasFile.find("flag")) {
            Team team = Team.ofKey(area.getName());
            if (team == null) {
                plugin().getLogger().severe("Invalid flag: " + area);
                continue;
            }
            teamMap.get(team).setFlagSpawn(area.getMin());
        }
        for (Area area : areasFile.find("merchant")) {
            if (area.getName() == null) {
                plugin().getLogger().severe("Merchant without name: " + area);
                continue;
            }
            merchantBlocks.put(area.getMin(), area.getName());
        }
        if (gameAreas.isEmpty()) throw new IllegalStateException("No 'game' areas!");
        if (creepAreas.isEmpty()) throw new IllegalStateException("No 'creep' areas!");
        for (GameTeam gameTeam : teamMap.values()) {
            Team team = gameTeam.getTeam();
            if (gameTeam.getSpawns().isEmpty()) {
                throw new IllegalStateException("No spawns for team: " + team);
            }
            if (gameTeam.getFlagSpawn() == null) {
                throw new IllegalStateException("No flag for team: " + team);
            }
        }
    }

    private void loadChunks() {
        Set<Vec2i> vecs = new HashSet<>();
        for (Cuboid area : gameAreas) {
            Cuboid cuboid = area.blockToChunk();
            for (int z = cuboid.az; z <= cuboid.bz; z += 1) {
                for (int x = cuboid.ax; x <= cuboid.bx; x += 1) {
                    vecs.add(Vec2i.of(x, z));
                }
            }
        }
        for (Vec2i vec : vecs) {
            world.getChunkAtAsync(vec.x, vec.z, (Consumer<Chunk>) chunk -> chunk.addPluginChunkTicket(plugin()));
        }
    }

    private void makeTeams() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            GamePlayer gp = new GamePlayer(player);
            playerMap.put(gp.getUuid(), gp);
        }
        List<GamePlayer> gpList = new ArrayList<>(playerMap.values());
        Collections.shuffle(gpList);
        for (int i = 0; i < gpList.size(); i += 1) {
            Team team = i < gpList.size() / 2 ? Team.RED : Team.BLUE;
            GameTeam gameTeam = teamMap.get(team);
            GamePlayer gamePlayer = gpList.get(i);
            gamePlayer.setGameTeam(gameTeam);
            gameTeam.getMembers().add(gamePlayer.getUuid());
        }
        for (GamePlayer gp : gpList) {
            Player player = gp.getPlayer();
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setFireTicks(0);
            player.setFallDistance(0f);
            player.setGameMode(GameMode.ADVENTURE);
            teleportToSpawn(player);
        }
    }

    public GameTeam getTeam(Player player) {
        GamePlayer gp = getGamePlayer(player);
        return gp != null ? gp.getGameTeam() : null;
    }

    public GamePlayer getGamePlayer(Player player) {
        return playerMap.get(player.getUniqueId());
    }

    private void teleportToSpawn(Player player) {
        GamePlayer gp = playerMap.get(player.getUniqueId());
        if (gp == null) throw new IllegalStateException("Player without GamePlayer: " + player.getName());
        if (gp.getGameTeam() == null) throw new IllegalStateException("Player without team: " + gp);
        List<Vec3i> spawns = findSpawnLocations(gp.getTeam());
        Vec3i vec = spawns.get(random.nextInt(spawns.size()));
        player.teleport(vec.toCenterFloorLocation(world));
    }

    protected List<Vec3i> findSpawnLocations(Team team) {
        List<Vec3i> result = new ArrayList<>();
        for (Cuboid cuboid : teamMap.get(team).getSpawns()) {
            for (Vec3i vec : cuboid.enumerate()) {
                Block block = vec.toBlock(world);
                if (block.isLiquid()) continue;
                if (block.getRelative(0, 1, 0).isLiquid()) continue;
                if (!block.getCollisionShape().getBoundingBoxes().isEmpty()) continue;
                if (!block.getRelative(0, 1, 0).getCollisionShape().getBoundingBoxes().isEmpty()) continue;
                Collection<BoundingBox> bbs = block.getRelative(0, -1, 0).getCollisionShape().getBoundingBoxes();
                if (bbs.size() != 1) continue;
                BoundingBox bb = bbs.iterator().next();
                if (bb.getHeight() == 1.0 && bb.getWidthX() == 1.0 && bb.getWidthZ() == 1.0) {
                    result.add(vec);
                }
            }
        }
        return result;
    }

    private void tick() {
        GameState newState = tickState();
        if (newState != null && newState != state) {
            onExitState(state);
            enterState(newState);
        }
        ticks += 1;
        stateTicks += 1;
    }

    private void onExitState(GameState oldState) {
        switch (oldState) {
        default: break;
        }
    }

    private void enterState(GameState newState) {
        state = newState;
        stateTicks = 0;
        switch (state) {
        case PLAY: {
            world.setDifficulty(Difficulty.HARD);
            for (Player player : world.getPlayers()) {
                player.showTitle(title(text("Go!", GREEN), text("The game begins", GREEN)));
                player.sendMessage(text("Go! The game begins", GREEN));
                if (getGamePlayer(player) == null) continue;
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.setFireTicks(0);
                player.setFallDistance(0f);
                player.setGameMode(GameMode.SURVIVAL);
                if (games().getSave().isEvent()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
                }
            }
            break;
        }
        case END: {
            world.setDifficulty(Difficulty.PEACEFUL);
            for (Player player : world.getPlayers()) {
                player.setGameMode(GameMode.ADVENTURE);
            }
            Team winningTeam = null;
            int red = teamMap.get(Team.RED).getScore();
            int blue = teamMap.get(Team.BLUE).getScore();
            if (red > blue) {
                winningTeam = Team.RED;
            } else if (blue > red) {
                winningTeam = Team.BLUE;
            }
            if (winningTeam == null) {
                announce(text("The game ends in a DRAW!", RED));
                announceTitle(text("Draw!", RED), text("Neither team wins", RED));
            } else {
                announce(text("Team " + winningTeam.displayName + " wins the game!", winningTeam.textColor));
                announceTitle(text(winningTeam.displayName, winningTeam.textColor), text("wins the game", winningTeam.textColor));
            }
            if (winningTeam != null && games().getSave().isEvent()) {
                for (UUID uuid : teamMap.get(winningTeam).getMembers()) {
                    games().getSave().addScore(uuid, 1);
                }
                games().save();
            }
            break;
        }
        default: break;
        }
    }

    public void onPlayerHud(PlayerHudEvent event) {
        List<Component> sidebar = new ArrayList<>();
        sidebar.add(textOfChildren(text(tiny("time"), GRAY), space(), timeComponent));
        for (Team team : Team.values()) {
            GameTeam gameTeam = teamMap.get(team);
            sidebar.add(textOfChildren(team.vanillaItem,
                                       (gameTeam.isFlagHome()
                                        ? Mytems.CHECKED_CHECKBOX.getCurrentAnimationFrame()
                                        : Mytems.CROSSED_CHECKBOX.getCurrentAnimationFrame()),
                                       space(),
                                       text(gameTeam.getScore(), team.textColor)));
        }
        event.sidebar(PlayerHudPriority.HIGH, sidebar);
    }

    public void onPlayerDeath(PlayerDeathEvent event) {
        if (state != GameState.PLAY) return;
        Player player = event.getPlayer();
        GameTeam playerTeam = getTeam(player);
        if (playerTeam == null) return;
        Player killer = player.getKiller();
        if (killer == null) return;
        GameTeam killerTeam = getTeam(killer);
        if (killerTeam == null) return;
        if (playerTeam.getTeam() == killerTeam.getTeam()) return;
        killer.getWorld().dropItem(killer.getLocation(), new ItemStack(Material.EMERALD, 10)).setPickupDelay(0);
        if (games().getSave().isEvent()) {
            games().getSave().addScore(killer.getUniqueId(), 1);
            games().computeHighscores();
        }
    }

    public void onEntityDeath(EntityDeathEvent event) {
        if (event instanceof PlayerDeathEvent) return;
        if (state != GameState.PLAY) return;
        if (event.getEntity().getKiller() == null) return;
        event.setDroppedExp(event.getDroppedExp() * 10);
        event.getDrops().add(new ItemStack(Material.EMERALD));
    }

    public void onEntityDamage(EntityDamageEvent event) {
        if (state != GameState.PLAY) {
            event.setCancelled(true);
            return;
        }
    }

    private GameState tickState() {
        return switch (state) {
        case INIT -> throw new IllegalStateException("state = INIT");
        case COUNTDOWN -> tickCountdown();
        case PLAY -> tickPlay();
        case END -> tickEnd();
        default -> throw new IllegalStateException("state = " + state);
        };
    }

    private GameState tickCountdown() {
        final int totalTicks = 60 * 20;
        final int ticksLeft = totalTicks - stateTicks;
        if (ticksLeft <= 0) return GameState.PLAY;
        final int seconds = ticksLeft / 20;
        timeComponent = text(seconds, GREEN);
        return null;
    }

    private GameState tickPlay() {
        for (Team team : Team.values()) {
            tickFlag(team);
        }
        for (Player player : world.getPlayers()) {
            GamePlayer gamePlayer = getGamePlayer(player);
            if (gamePlayer != null) tickPlayer(player, gamePlayer);
        }
        int seconds = (GAME_TIME_TICKS - stateTicks) / 20;
        int minutes = seconds / 60;
        timeComponent = textOfChildren(text(minutes, GREEN), text("m", GRAY),
                                       space(),
                                       text((seconds % 60), GREEN), text("s", GRAY));
        if (stateTicks > GAME_TIME_TICKS) return GameState.END;
        spawnCreep();
        return null;
    }

    private void tickFlag(Team team) {
        GameTeam gameTeam = teamMap.get(team);
        GameFlag gameFlag = gameTeam.getGameFlag();
        if (gameFlag == null) {
            gameFlag = new GameFlag(team);
            gameFlag.spawn(gameTeam.getFlagSpawn().toCenterLocation(world));
            gameTeam.setGameFlag(gameFlag);
        } else if (!gameFlag.isValid()) {
            gameFlag.disable();
            gameTeam.setGameFlag(null);
        } else if (gameFlag.getHolder() != null) {
            gameFlag.setNoHolderTicks(0);
            Player holder = Bukkit.getPlayer(gameFlag.getHolder());
            if (holder == null || holder.isDead()) {
                announce(text(PlayerCache.nameForUuid(gameFlag.getHolder()) + " dropped the " + team.displayName + " flag", YELLOW));
                gameFlag.setHolder(null);
            } else {
                GamePlayer gameHolder = getGamePlayer(holder);
                if (gameHolder == null) {
                    gameFlag.setHolder(null);
                } else {
                    Location location = holder.getEyeLocation().add(0.0, 1.0, 0.0);
                    location.setPitch(0.0f);
                    location.setYaw((float) stateTicks * 9f);
                    gameFlag.teleport(location);
                }
            }
        } else if (gameFlag.getHolder() == null && !gameTeam.isFlagHome() && gameFlag.getNoHolderTicks() > 20 * 60) {
            gameFlag.disable();
            gameTeam.setGameFlag(null);
            announce(text("The " + team.displayName + " flag returned home", YELLOW));
        }
    }

    private void tickPlayer(Player player, GamePlayer gamePlayer) {
        tickPlayerBoundingBox(player, gamePlayer);
    }

    private void tickPlayerBoundingBox(Player player, GamePlayer gamePlayer) {
        final UUID uuid = player.getUniqueId();
        final GameTeam playerTeam = gamePlayer.getGameTeam();
        GameFlag holdingFlag = null;
        for (GameTeam gameTeam : teamMap.values()) {
            GameFlag gameFlag = gameTeam.getGameFlag();
            if (gameFlag == null) continue;
            if (!uuid.equals(gameFlag.getHolder())) continue;
            holdingFlag = gameFlag;
        }
        BoundingBox bb = player.getBoundingBox();
        final Vector min = bb.getMin();
        final Vector max = bb.getMax();
        final int ax = min.getBlockX();
        final int ay = min.getBlockY();
        final int az = min.getBlockZ();
        final int bx = max.getBlockX();
        final int by = max.getBlockY();
        final int bz = max.getBlockZ();
        for (int y = ay; y <= by; y += 1) {
            for (int z = az; z <= bz; z += 1) {
                for (int x = ax; x <= bx; x += 1) {
                    for (Team team : Team.values()) {
                        GameTeam gameTeam = teamMap.get(team);
                        GameFlag gameFlag = gameTeam.getGameFlag();
                        if (gameFlag != null && gameFlag.getHolder() == null && gameFlag.getVector().equals(x, y, z)) {
                            if (gameFlag.getTeam() == playerTeam.getTeam() && !gameTeam.isFlagHome()) {
                                announce(text(player.getName() + " returned the " + team.displayName + " flag", YELLOW));
                                gameFlag.disable();
                                gameTeam.setGameFlag(null);
                            } else if (gameFlag.getTeam() != playerTeam.getTeam()) {
                                announce(text(player.getName() + " picked up the " + team.displayName + " flag", YELLOW));
                                gameFlag.setHolder(uuid);
                            }
                        }
                    }
                    if (holdingFlag != null && playerTeam.getFlagSpawn().equals(x, y, z) && playerTeam.isFlagHome()) {
                        GameTeam gameTeam = teamMap.get(holdingFlag.getTeam());
                        if (gameTeam.getGameFlag() != null) {
                            gameTeam.getGameFlag().disable();
                            gameTeam.setGameFlag(null);
                        }
                        announce(text(player.getName() + " stole the " + holdingFlag.getTeam().displayName + " flag", playerTeam.getTeam().getTextColor()));
                        playerTeam.setScore(playerTeam.getScore() + 1);
                        if (games().getSave().isEvent()) {
                            games().getSave().addScore(player.getUniqueId(), 10);
                            games().computeHighscores();
                            games().save();
                        }
                    }
                }
            }
        }
    }

    private Entity spawnCreep() {
        if (creepAreas.isEmpty()) return null;
        Cuboid area = creepAreas.get(random.nextInt(creepAreas.size()));
        int x = area.ax + random.nextInt(area.getSizeX());
        int z = area.az + random.nextInt(area.getSizeZ());
        for (int y = area.by; y >= area.ay; y -= 1) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.WATER) {
                for (int dy = -1; dy <= 0; dy += 1) {
                    for (int dz = -1; dz <= 1; dz += 1) {
                        for (int dx = -1; dx <= 1; dx += 1) {
                            if (block.getRelative(dx, dy, dz).getType() != Material.WATER) return null;
                        }
                    }
                }
                Location location = block.getLocation().add(0.5, 0.5, 0.5);
                if (location.getNearbyEntitiesByType(Mob.class, 32.0).size() > 3) return null;
                return world.spawn(location, Guardian.class, e -> { }, SpawnReason.CUSTOM);
            }
            // Not liquid
            var voxelShape = block.getCollisionShape();
            if (voxelShape.getBoundingBoxes().size() != 1) continue;
            var bb = voxelShape.getBoundingBoxes().iterator().next();
            if (bb.getHeight() != 1.0) continue;
            if (!block.getRelative(0, 1, 0).getCollisionShape().getBoundingBoxes().isEmpty()) return null;
            if (!block.getRelative(0, 2, 0).getCollisionShape().getBoundingBoxes().isEmpty()) return null;
            List<EntityType> types = List.of(EntityType.ZOMBIE,
                                             EntityType.CREEPER,
                                             EntityType.SKELETON,
                                             EntityType.SPIDER,
                                             EntityType.WITCH,
                                             EntityType.ENDERMAN,
                                             EntityType.SLIME,
                                             EntityType.WITHER_SKELETON,
                                             EntityType.BLAZE);
            EntityType et = types.get(random.nextInt(types.size()));
            Location location = block.getLocation().add(0.5, 1.0, 0.5);
            if (location.getNearbyEntitiesByType(Mob.class, 32.0).size() > 4) return null;
            return world.spawnEntity(location, et, SpawnReason.CUSTOM, e -> {
                    if (e instanceof Zombie zombie) zombie.setShouldBurnInDay(false);
                    if (e instanceof AbstractSkeleton skeleton) skeleton.setShouldBurnInDay(false);
                });
        }
        return null;
    }

    private GameState tickEnd() {
        if (stateTicks > 20 * 60) {
            disable();
        }
        return null;
    }
}
