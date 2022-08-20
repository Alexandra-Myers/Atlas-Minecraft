package net.atlas.minecraft.common.registry;

import net.atlas.minecraft.common.item.Item;

import java.util.ArrayList;

public @interface ItemRegistry {
    String addItem() default "minecraft:air";
}
