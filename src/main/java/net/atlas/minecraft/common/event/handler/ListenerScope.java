package net.atlas.minecraft.common.event.handler;

import java.lang.annotation.*;

/**
 * Specifies the listener for a certain scope (e.g. A.B) or scope group (e.g. A.*)
 *
 * @see net.atlas.minecraft.common.event.scope.ScopeGroup
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ListenerScope {

    /**
     * @return the handler's scope or scope group
     */
    String value();
}
