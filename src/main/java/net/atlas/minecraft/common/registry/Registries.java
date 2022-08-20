package net.atlas.minecraft.common.registry;

import net.atlas.minecraft.common.item.Item;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

public class Registries {
    public static RegistryKey<Item> itemRegistry = new RegistryKey<>();
    public Registries() {
        Air();
        WoodenSword();
    }
    @ItemRegistry()
    public final Item Air() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.add("minecraft:air", 0, thisItem);
        return thisItem;
    }
    @ItemRegistry(addItem = "minecraft:wooden_sword")
    public final Item WoodenSword() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.add("minecraft:wooden_sword", 1, thisItem);
        return thisItem;
    }
    public static void init() {
    }
}
