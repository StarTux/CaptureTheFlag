package com.cavetale.capturetheflag;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.mytems.MytemsPlugin.mytemsPlugin;
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
        result.add(a != null ? deserialize2(a) : null);
        result.add(b != null ? deserialize2(b) : null);
        result.add(c != null ? deserialize2(c) : null);
        return result;
    }

    private ItemStack deserialize2(String in) {
        if (in == null) return null;
        final ItemStack result = deserialize(in);
        if (result == null) return null;
        final ItemStack result2 = mytemsPlugin().fixItemStack(result);
        return result2 != null
            ? result2
            : result;
    }
}
