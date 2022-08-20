package net.atlas.minecraft.common.event.handler;

import java.lang.annotation.*;

/**
 * A typed listener only listens to a specific type of a typed event. The type is defined by an integer constant, which
 * identifies the type
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventType {

    /**
     * @return the type identifier that shall be listened to
     */
    int value();
}
