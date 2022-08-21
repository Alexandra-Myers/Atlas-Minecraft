package net.atlas.minecraft.common.registry;

import net.atlas.minecraft.common.item.Item;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Registries {
    public static RegistryKey<Item> itemRegistry = new RegistryKey<>(40);
    public Registries() throws Exception {
        initializeObject(this);
        init(Logger.getGlobal());
    }
    @ItemRegistry()
    public final RegistryKey<Item> Air() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.setElement(thisItem);
        return itemRegistry;
    }
    @ItemRegistry(addItem = "minecraft:wooden_sword", addID = 1)
    public final RegistryKey<Item> WoodenSword() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.setElement(thisItem);
        return itemRegistry;
    }
    public void init(Logger logger) {
        logger.log(Level.INFO, "Type: " + Air().ID.type);
        logger.log(Level.INFO, "Type: " + WoodenSword().ID.type);
        logger.log(Level.INFO, "Fireproof: " + WoodenSword().get(1).isFireproof());
    }
    private void initializeObject(Object object) throws Exception {
        Class<?> clazz = object.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ItemRegistry.class)) {
                method.setAccessible(true);
                method.invoke(object);
                itemRegistry.add(method.getAnnotation(ItemRegistry.class).addItem(), method.getAnnotation(ItemRegistry.class).addID());
            }
        }
    }
}
