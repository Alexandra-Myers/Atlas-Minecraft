package net.atlas.minecraft.common.event.exception;

import java.lang.reflect.InvocationTargetException;

public class EventDispatchException extends RuntimeException {

    public EventDispatchException(final String message, final InvocationTargetException cause) {
        super(message, cause);
    }
}
