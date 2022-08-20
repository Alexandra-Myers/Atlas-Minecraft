package net.atlas.minecraft.common.event.handler;

import java.lang.annotation.*;

/**
 * This annotation marks methods, that get called by the manager when an event of the method argument type rises.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /**
     * The priority of the listener method. The highest priority listeners are called first, while lowest priority are
     * called last. By default, priority is set to normal.
     *
     * @return Event Priority.
     */
    ListenerPriority priority() default ListenerPriority.NORMAL;
}
