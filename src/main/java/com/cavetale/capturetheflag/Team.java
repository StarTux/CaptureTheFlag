package com.cavetale.capturetheflag;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;

@RequiredArgsConstructor
public enum Team {
    RED("Red", NamedTextColor.RED),
    BLUE("Blue", NamedTextColor.BLUE),
    ;

    public final String key = name().toLowerCase();
    public final String displayName;
    public final NamedTextColor color;

    public static Team ofKey(String in) {
        for (Team team : Team.values()) {
            if (in.equals(team.key)) return team;
        }
        return null;
    }
}
