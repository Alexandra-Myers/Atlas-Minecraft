package net.atlas.minecraft.common.event.exception;

public class EventReceivedInterruptException extends RuntimeException {

    public EventReceivedInterruptException(final int value) {
        super("event success: " + value);
    }
}
