package net.atlas.minecraft.common.registry;

import java.util.ArrayList;
import java.util.Iterator;

public class RegistryKey<T> extends ArrayList<T> implements Key<T>{
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
        ID = new Identifier<>(name, element);
        super.add(index, element);
    }
    public void add(String namespace, String path, int index) {
        ID = new Identifier<>(namespace, path, element);
        super.add(index, element);
    }
    public void add(String name) {
        ID = new Identifier<>(name, element);
        super.add(element);
    }
    public void add(String namespace, String path) {
        ID = new Identifier<>(namespace, path, element);
        super.add(element);
    }

    @Override
    public Key<T> getFromID(Identifier identifier) {
        boolean bl = ID == identifier;
        for (Iterator<T> it = this.iterator(); it.hasNext(); ) {
            T thisKey = it.next();
            if(thisKey instanceof Key<?>) {
                return (Key<T>) thisKey;
            }
        }
        return null;
    }

    @Override
    public Identifier setID(String name, T type) {
        ID = new Identifier<>(name, type);
        return ID;
    }
}
