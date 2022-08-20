package net.atlas.minecraft.common.item;

import net.atlas.minecraft.common.registry.RegistryKey;

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
        public final RegistryKey<Boolean> boolSettings;
        public Settings(boolean fireproof) {
            this.boolSettings = new RegistryKey<>();
            boolSettings.addProperty("minecraft:fireproof", 0, fireproof);
        }
        public RegistryKey<Boolean> getBoolSettings() {
            return boolSettings;
        }
    }
}
