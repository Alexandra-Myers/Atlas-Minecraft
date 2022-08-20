package net.atlas.minecraft.common.registry;

public class Identifier {
    public final int RAW_ID;
    public final String ID;
    public static volatile Identifier instance;
    Identifier(String name, int index) {
        RAW_ID = index;
        ID = name;
    }
    Identifier(String namespace, String path, int index) {
        RAW_ID = index;
        ID = namespace + ":" + path;
    }
    public static Identifier getInstance(String namespace, String path, int index) {
        if (instance == null) {
            synchronized(Identifier.class) {
                if (instance == null) {
                    instance = new Identifier(namespace, path, index);
                }
            }
        }

        return instance;
    }
    public static Identifier getInstance(String name, int index) {
        if (instance == null) {
            synchronized(Identifier.class) {
                if (instance == null) {
                    instance = new Identifier(name, index);
                }
            }
        }

        return instance;
    }
}
