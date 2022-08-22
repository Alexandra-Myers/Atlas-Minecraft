package net.atlas.minecraft.common.registry;

public class Identifier<T> {
    public final String ID;
    public final String NAMESPACE;
    public final String PATH;
    public final T type;
    Identifier(String name, T type) {
        int i = name.indexOf(':');
        String[] strings = i >= 1 ? new String[]{name.substring(0, i), name.substring(i + 1)} : i == 0 ? new String[]{"minecraft", name.substring(i + 1)} : new String[]{"minecraft", name};
        NAMESPACE = strings[0];
        PATH = strings[1];
        ID = NAMESPACE + ":" + PATH;
        this.type = type;
    }
    Identifier(String name, char delimiter, T type) {
        int i = name.indexOf(delimiter);
        String[] strings = i >= 1 ? new String[]{name.substring(0, i), name.substring(i + 1)} : i == 0 ? new String[]{"minecraft", name.substring(i + 1)} : new String[]{"minecraft", name};
        NAMESPACE = strings[0];
        PATH = strings[1];
        ID = NAMESPACE + ":" + PATH;
        this.type = type;
    }
    Identifier(String namespace, String path, T type) {
        ID = namespace + ":" + path;
        NAMESPACE = namespace;
        PATH = path;
        this.type = type;
    }
//    Identifier(String name, int index, T type) {
//        RAW_ID = index;
//        int i = name.indexOf(':');
//        String[] strings = i >= 1 ? new String[]{name.substring(0, i), name.substring(i + 1)} : i == 0 ? new String[]{"minecraft", name.substring(i + 1)} : new String[]{"minecraft", name};
//        NAMESPACE = strings[0];
//        PATH = strings[1];
//        ID = NAMESPACE + ":" + PATH;
//        this.type = type;
//    }
//    Identifier(String name, int index, char delimiter, T type) {
//        RAW_ID = index;
//        int i = name.indexOf(delimiter);
//        String[] strings = i >= 1 ? new String[]{name.substring(0, i), name.substring(i + 1)} : i == 0 ? new String[]{"minecraft", name.substring(i + 1)} : new String[]{"minecraft", name};
//        NAMESPACE = strings[0];
//        PATH = strings[1];
//        ID = NAMESPACE + ":" + PATH;
//        this.type = type;
//    }
//    Identifier(String namespace, String path, int index, T type) {
//        RAW_ID = index;
//        ID = namespace + ":" + path;
//        NAMESPACE = namespace;
//        PATH = path;
//        this.type = type;
//    }
}
