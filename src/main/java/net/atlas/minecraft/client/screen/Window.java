package net.atlas.minecraft.client.screen;

import net.atlas.minecraft.client.camera.Camera;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    public int width,height;
    public String title;

    private long window;

    public Camera camera;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void initWindow() {
        glfwInit();

        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        window = glfwCreateWindow(width, height, title, 0, 0);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        glfwShowWindow(window);

        camera = new Camera(0,0,0);
    }
}
