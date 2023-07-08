package com.cavetale.capturetheflag;

import com.cavetale.core.struct.Vec3i;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

/**
 * Game runtime of a flag.
 */
@Data @RequiredArgsConstructor
public final class GameFlag {
    private final Team team;
    private Vec3i vector;
    private ItemDisplay entity;
    private UUID holder;
    private int noHolderTicks;

    public boolean isValid() {
        return entity != null && entity.isValid();
    }

    public void disable() {
        if (entity != null) {
            entity.remove();
            entity = null;
        }
    }

    public void spawn(Location location) {
        this.entity = location.getWorld().spawn(location, ItemDisplay.class, e -> {
                e.setItemStack(new ItemStack(team.getDyeMaterial()));
            });
        this.vector = Vec3i.of(location);
    }

    public void teleport(Location location) {
        entity.teleport(location);
        this.vector = Vec3i.of(location);
    }
}
