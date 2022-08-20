package net.atlas.minecraft.common.event.events;

import net.atlas.minecraft.common.event.Event;

public class TestEventBase extends Event {
    private final int value;

    public TestEventBase(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
