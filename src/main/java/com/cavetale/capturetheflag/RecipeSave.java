package com.cavetale.capturetheflag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public final class RecipeSave {
    private Map<RecipeType, List<Recipe>> recipes = new HashMap<>();

    public List<Recipe> get(RecipeType type) {
        return recipes.computeIfAbsent(type, t -> new ArrayList<>());
    }
}
