package com.cavetale.capturetheflag;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Items {
    private static Items instance;
    protected ItemStack landMine = new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);

    public Items() {
        instance = this;
    }

    protected void enable() {
        landMine.editMeta(meta -> {
                meta.displayName(text("Land Mine", RED));
                meta.getPersistentDataContainer().set(new NamespacedKey(CaptureTheFlagPlugin.plugin(), "land_mine"),
                                                      PersistentDataType.BYTE, (byte) 1);
            });
    }

    public static Items items() {
        return instance;
    }
}
