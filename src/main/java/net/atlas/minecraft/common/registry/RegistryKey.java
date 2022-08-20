package net.atlas.minecraft.common.registry;

import java.util.ArrayList;

public class RegistryKey<T> extends ArrayList<T> {
    RegistryKey() {

    }
    public void add(String name, int index, T element) {
        new Identifier(name, index);
        super.add(index, element);
    }
    public void add(String namespace, String path, int index, T element) {
        new Identifier(namespace, path, index);
        super.add(index, element);
    }
}