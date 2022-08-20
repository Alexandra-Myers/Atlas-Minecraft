package net.atlas.minecraft.common.registry;

import net.atlas.minecraft.common.item.Item;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Registries {
    public static RegistryKey<Item> itemRegistry = new RegistryKey<>();
    public Registries() {
        Air();
        WoodenSword();
        init(Logger.getGlobal());
    }
    @ItemRegistry()
    public final RegistryKey<Item> Air() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.add("minecraft:air", 0, thisItem);
        return itemRegistry;
    }
    @ItemRegistry(addItem = "minecraft:wooden_sword")
    public final RegistryKey<Item> WoodenSword() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.add("minecraft", "wooden_sword", 1, thisItem);
        return itemRegistry;
    }
    public void init(Logger logger) {
        logger.log(Level.INFO, WoodenSword().getID().ID);
        logger.log(Level.INFO, "Fireproof: " + WoodenSword().get(1).isFireproof());
    }
}
