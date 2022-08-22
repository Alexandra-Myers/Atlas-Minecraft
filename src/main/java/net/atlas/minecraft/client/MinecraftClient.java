package net.atlas.minecraft.client;



import net.atlas.minecraft.client.screen.Window;
import net.atlas.minecraft.common.event.EventManager;
import net.atlas.minecraft.common.event.events.TestEventBase;
import net.atlas.minecraft.common.event.events.TestListener;
import net.atlas.minecraft.common.event.exception.EventScopeException;
import net.atlas.minecraft.common.registry.Registries;

public class MinecraftClient {

    public static Window window = new Window(1080,720,"Minecraft");

    private static final TestListener testListener1 = new TestListener();

    public static void main(String[] argv) {

        window.initWindow();

        EventManager.ERROR_POLICY = EventScopeException.EXCEPTION;
        EventManager.registerListeners(testListener1);

        EventManager.callEvent(new TestEventBase(42));
    }



}
