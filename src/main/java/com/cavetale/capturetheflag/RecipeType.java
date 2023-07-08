package com.cavetale.capturetheflag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Villager.Profession;
import static net.kyori.adventure.text.Component.text;

@Getter @RequiredArgsConstructor
public enum RecipeType {
    ARMOR(text("Armor"), Profession.ARMORER),
    WEAPON(text("Weapons"), Profession.WEAPONSMITH),
    SUPPLY(text("Supplies"), Profession.FARMER),
    ;

    public final Component title;
    public final Profession profession;

    public static RecipeType of(String in) {
        try {
            return valueOf(in.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }
}
