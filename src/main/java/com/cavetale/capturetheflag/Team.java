package com.cavetale.capturetheflag;

import com.cavetale.core.font.VanillaItems;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import static net.kyori.adventure.text.Component.text;

@Getter @RequiredArgsConstructor
public enum Team {
    RED("Red", NamedTextColor.RED, Material.RED_BANNER, VanillaItems.RED_CONCRETE),
    BLUE("Blue", NamedTextColor.BLUE, Material.BLUE_BANNER, VanillaItems.BLUE_CONCRETE),
    ;

    public final String key = name().toLowerCase();
    public final String displayName;
    public final NamedTextColor textColor;
    public final Material dyeMaterial;
    public final VanillaItems vanillaItem;

    public static Team ofKey(String in) {
        for (Team team : Team.values()) {
            if (in.equals(team.key)) return team;
        }
        return null;
    }

    public Color getColor() {
        return Color.fromRGB(textColor.value());
    }

    public Component displayComponent() {
        return text(displayName, textColor);
    }
}
