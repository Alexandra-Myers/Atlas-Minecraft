package net.atlas.minecraft.common.registry;

import net.atlas.minecraft.common.item.Item;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ItemRegistry {
    String addItem() default "minecraft:air";
    int addID() default 0;
}
