package net.atlas.minecraft.common.registry;

import net.atlas.minecraft.common.item.Item;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Registries {
    public static RegistryKey<Item> itemRegistry = new RegistryKey<>(2);
    public Registries() throws InvocationTargetException, IllegalAccessException {
        initializeObject(this);
        init(Logger.getGlobal());
    }
    @ItemRegistry()
    public final RegistryKey<Item> Air() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.setElement(thisItem);
        return itemRegistry;
    }
    @ItemRegistry(addItem = "minecraft:wooden_sword")
    public final RegistryKey<Item> WoodenSword() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.setElement(thisItem);
        return itemRegistry;
    }
    @ItemRegistry(addItem = "minecraft:stone_sword")
    public final RegistryKey<Item> StoneSword() {
        Item thisItem = new Item(new Item.Settings(false));
        itemRegistry.setElement(thisItem);
        return itemRegistry;
    }
    public void init(Logger logger) {

    }
    public void initializeObject(Object object) throws InvocationTargetException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ItemRegistry.class)) {
                method.setAccessible(true);
                method.invoke(object);
                itemRegistry.add(method.getAnnotation(ItemRegistry.class).addItem());
                Logger.getGlobal().log(Level.INFO, "Name: " + method.getAnnotation(ItemRegistry.class).addItem());
                Logger.getGlobal().log(Level.INFO, "Raw ID: " + method.getAnnotation(ItemRegistry.class).addID());
            }
        }
    }
}
