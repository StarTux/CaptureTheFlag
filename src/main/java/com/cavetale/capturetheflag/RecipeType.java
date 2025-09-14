package com.cavetale.capturetheflag;

import com.cavetale.core.font.VanillaItems;
import com.cavetale.mytems.Mytems;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Villager.Profession;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter @RequiredArgsConstructor
public enum RecipeType {
    ARMOR("Armor", Profession.ARMORER, VanillaItems.DIAMOND, AQUA),
    WEAPON("Weapons", Profession.WEAPONSMITH, Mytems.RUBY, RED),
    SUPPLY("Supplies", Profession.FARMER, VanillaItems.AMETHYST_SHARD, LIGHT_PURPLE),
    ;

    private final String displayName;
    private final Profession profession;
    private final ComponentLike chatComponent;
    private final TextColor textColor;

    public static RecipeType of(String in) {
        try {
            return valueOf(in.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public Component getComponent() {
        return chatComponent.asComponent();
    }
}
