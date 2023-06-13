package com.cavetale.capturetheflag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public final class Save {
    private boolean event;
    private boolean debug;
    private Map<UUID, Integer> scores = new HashMap<>();

    public void addScore(UUID uuid, int value) {
        scores.put(uuid, Math.max(0, scores.getOrDefault(uuid, 0) + value));
    }

    public int getScore(UUID uuid) {
        return scores.getOrDefault(uuid, 0);
    }
}
