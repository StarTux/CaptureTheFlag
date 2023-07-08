package com.cavetale.capturetheflag;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.mytems.util.Items.deserialize;
import static com.cavetale.mytems.util.Items.serializeToBase64;

public final class Recipe {
    private String a;
    private String b;
    private String c;

    public void setItems(ItemStack ia, ItemStack ib, ItemStack ic) {
        this.a = ia != null
            ? serializeToBase64(ia)
            : null;
        this.b = ib != null
            ? serializeToBase64(ib)
            : null;
        this.c = ic != null
            ? serializeToBase64(ic)
            : null;
    }

    public List<ItemStack> getItems() {
        List<ItemStack> result = new ArrayList<>();
        result.add(a != null ? deserialize(a) : null);
        result.add(b != null ? deserialize(b) : null);
        result.add(c != null ? deserialize(c) : null);
        return result;
    }
}
