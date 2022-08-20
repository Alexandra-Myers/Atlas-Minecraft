package net.atlas.minecraft.common.event.events;

import net.atlas.minecraft.common.event.exception.EventReceivedInterruptException;
import net.atlas.minecraft.common.event.handler.EventHandler;

public class TestListener {

    @EventHandler
    public void onTest(final TestEventBase event) {
        throw new EventReceivedInterruptException(event.getValue());
    }

}
