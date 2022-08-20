package net.atlas.minecraft.common.item;

public class Item {
    public final Boolean[] boolProperties;
    public Item(Settings settings) {
        this.boolProperties = settings.getBoolSettings();
    }
    public static class Settings {
        public final Boolean[] boolSettings;
        public Settings(boolean fireproof) {
            this.boolSettings = new Boolean[] {fireproof};
        }
        public Boolean[] getBoolSettings() {
            return boolSettings;
        }
    }
}
