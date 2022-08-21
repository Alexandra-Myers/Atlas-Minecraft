package net.atlas.minecraft.common.registry;

import java.util.ArrayList;

public class RegistryKey<T> extends ArrayList<T> {
    public Identifier<T> ID;
    public RegistryKey() {
        super();
    }
    public RegistryKey(int initialMaxIndex) {
        super(initialMaxIndex);
    }
    public T element;

    public void setElement(T element) {
        this.element = element;
    }

    public void add(String name, int index) {
        ID = new Identifier<>(name, index, element);
        super.add(index, element);
    }
    public void add(String namespace, String path, int index) {
        ID = new Identifier<>(namespace, path, index, element);
        super.add(index, element);
    }
}
