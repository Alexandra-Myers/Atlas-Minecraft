package net.atlas.minecraft.common.event;

public abstract class Event {

    private boolean cancelled;

    /**
     * @return whether the event was cancelled
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Set the cancel status of this event.
     * A cancelled event will be handed to all listeners although it has been cancelled before. Other listeners can undo
     * the cancel operation.
     *
     * @param cancelled whether this event shall be cancelled
     */
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}
