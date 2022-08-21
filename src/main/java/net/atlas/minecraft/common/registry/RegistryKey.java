package net.atlas.minecraft.common.registry;

import java.util.ArrayList;

public class RegistryKey<T> extends ArrayList<T> {
    private Identifier ID;
    public RegistryKey() {

    }
    public void add(String name, int index, T element) {
        ID = new Identifier<T>(name, index, (T) element.getClass());
        super.add(index, element);
    }
    public void add(String namespace, String path, int index, T element) {
        ID = new Identifier<T>(namespace, path, index, (T) element.getClass());
        super.add(index, element);
    }
    public Identifier getID() {
        return ID;
    }
}
