package com.cavetale.capturetheflag;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.capturetheflag.world.Worlds;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import static com.cavetale.capturetheflag.CaptureTheFlagPlugin.plugin;

@Getter
public final class Game {
    private final String mapName;
    private String loadedWorldName; // loaded world name
    private World world;
    private List<Cuboid> gameAreas = new ArrayList<>();
    private Map<Team, List<Cuboid>> teamSpawns = new HashMap<>();
    private Map<Vec3i, String> merchantBlocks = new HashMap<>();
    private BukkitTask task;
    private Map<UUID, GamePlayer> playerMap = new HashMap<>();
    private Random random = new Random();

    public Game(final String mapName) {
        this.mapName = mapName;
    }

    public void enable() {
        loadWorld();
        if (world == null) {
            throw new IllegalStateException("World didn't load: " + mapName);
        }
        loadAreas();
        loadChunks();
        makeTeams();
        Bukkit.getScheduler().runTaskTimer(plugin(), this::tick, 1L, 1L);
    }

    public void disable() {
        if (world != null) {
            world.removePluginChunkTickets(plugin());
            Worlds.deleteWorld(world);
            world = null;
        }
        if (task != null) {
            task.cancel();
        }
    }

    private void loadWorld() {
        this.world = Worlds.loadWorldCopy(mapName);
        if (world == null) throw new IllegalStateException("Loading world " + mapName);
        this.loadedWorldName = world.getName();
    }

    private void loadAreas() {
        for (Team team : Team.values()) {
            teamSpawns.put(team, new ArrayList<>());
        }
        AreasFile areasFile = AreasFile.load(world, "CaptureTheFlag");
        if (areasFile == null) {
            throw new IllegalStateException("No areas file: " + mapName);
        }
        for (Area area : areasFile.find("game")) {
            gameAreas.add(area.toCuboid());
        }
        for (Area area : areasFile.find("spawn")) {
            Team team = Team.ofKey(area.getName());
            if (team == null) {
                plugin().getLogger().severe("Invalid spawn: " + area);
                continue;
            }
            teamSpawns.get(team).add(area.toCuboid());
        }
        for (Area area : areasFile.find("merchant")) {
            if (area.getName() == null) {
                plugin().getLogger().severe("Merchant without name: " + area);
                continue;
            }
            merchantBlocks.put(area.getMin(), area.getName());
        }
    }

    private void loadChunks() {
        Set<Vec2i> vecs = new HashSet<>();
        for (Cuboid area : gameAreas) {
            Cuboid cuboid = area.blockToChunk();
            for (int z = cuboid.az; z <= cuboid.bz; z += 1) {
                for (int x = cuboid.ax; z <= cuboid.bx; x += 1) {
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
            gpList.get(i).setTeam(i < gpList.size() / 2 ? Team.RED : Team.BLUE);
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

    private void teleportToSpawn(Player player) {
        GamePlayer gp = playerMap.get(player.getUniqueId());
        if (gp == null) throw new IllegalStateException("Player without GamePlayer: " + player.getName());
        if (gp.getTeam() == null) throw new IllegalStateException("Player without team: " + gp);
        List<Vec3i> spawns = findSpawnLocations(gp.getTeam());
        Vec3i vec = spawns.get(random.nextInt(spawns.size()));
        player.teleport(vec.toCenterFloorLocation(world));
    }

    protected List<Vec3i> findSpawnLocations(Team team) {
        List<Vec3i> result = new ArrayList<>();
        for (Cuboid cuboid : teamSpawns.get(team)) {
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
    }
}
