package net.atlas.minecraft.common.registry;

public interface Key<T> {
    Key<T> getFromID(Identifier identifier);
    Identifier setID(String name, T type);
}
