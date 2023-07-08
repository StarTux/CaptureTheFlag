package com.cavetale.capturetheflag;

import com.cavetale.core.struct.Vec3i;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Villager;

@Data @RequiredArgsConstructor
public final class MerchantBlock {
    private final Vec3i vector;
    private final RecipeType type;
    private Villager entity;
}
