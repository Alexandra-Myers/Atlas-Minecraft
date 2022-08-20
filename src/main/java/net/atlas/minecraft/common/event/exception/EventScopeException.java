package net.atlas.minecraft.common.event.exception;

public enum EventScopeException {

    /**
     * Throw a {@link EventDispatchException} on errors in dispatched events
     */
    EXCEPTION,

    /**
     * Output error only in log
     */
    LOG;

}
