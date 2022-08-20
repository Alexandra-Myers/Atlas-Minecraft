package net.atlas.minecraft.common.registry;

public class Identifier {
    public final int RAW_ID;
    public final String ID;
    public final String NAMESPACE;
    public final String PATH;
    Identifier(String name, int index) {
        RAW_ID = index;
        ID = name;
        int i = name.indexOf(':');
        String[] strings = new String[]{name.substring(0, i), name.substring(i + 1, name.length())};
        String namespace = strings[0];
        String path = strings[1];
        NAMESPACE = namespace;
        PATH = path;
    }
    Identifier(String name, int index, char delimiter) {
        RAW_ID = index;
        ID = name;
        int i = name.indexOf(delimiter);
        String[] strings = i >= 1 ? new String[]{name.substring(0, i - 1), name.substring(i + 1)} : i == 0 ? new String[]{"minecraft", name.substring(i + 1)} : new String[]{"minecraft", name};
        String namespace = strings[0];
        String path = strings[1];
        NAMESPACE = namespace;
        PATH = path;
    }
    Identifier(String namespace, String path, int index) {
        RAW_ID = index;
        ID = namespace + ":" + path;
        NAMESPACE = namespace;
        PATH = path;
    }
}
