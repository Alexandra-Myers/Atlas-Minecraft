package net.atlas.minecraft.common.item;

import net.atlas.minecraft.common.registry.ItemRegistry;
import net.atlas.minecraft.common.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Item implements ItemLike {
    public final RegistryKey<Boolean> boolProperties;
    public Item(Settings settings) {
        this.boolProperties = settings.getBoolSettings();
    }
    public boolean isFireproof() {
        return boolProperties.get(0);
    }

    @Override
    public Item asItem() {
        return this;
    }

    public static class Settings {
//        public int maxStackSize = 64;
//        int maxDamage;
//        @Nullable
//        Item craftingRemainingItem;
//        @Nullable
//        CreativeModeTab category;
//        Rarity rarity = Rarity.COMMON;
//        @Nullable
//        FoodProperties foodProperties;
        public final RegistryKey<Boolean> boolSettings;
        final boolean isFireproof;
        public Settings(boolean fireproof) {
            this.boolSettings = new RegistryKey<>();
            isFireproof = fireproof;
            Fireproof();
        }
        public final RegistryKey<Boolean> Fireproof() {
            boolSettings.add("minecraft:fireproof", 0, isFireproof);
            return boolSettings;
        }
        public RegistryKey<Boolean> getBoolSettings() {
            return boolSettings;
        }
    }
}
