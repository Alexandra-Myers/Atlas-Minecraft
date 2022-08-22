package net.atlas.minecraft.client.screen;

import net.atlas.minecraft.client.camera.Camera;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

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
        GLFW.glfwInit();
        window = GLFW.glfwCreateWindow(width,height,title,GLFW.GLFW_PLATFORM_NULL,0);

        if(window == 0) {
            throw new RuntimeException("GLFW Window could not be initialized");
        }

        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

        GLFW.glfwSetWindowPos(window,(videoMode.width() - this.width) / 2,(videoMode.height() - this.height));

        GLFW.glfwMakeContextCurrent(window);

        GLFW.glfwShowWindow(window);

        GLFW.glfwSwapInterval(1);

        camera = new Camera(0,0,0);

        while(true) {
            update();
            render();
        }
    }

    public void update() {
        GLFW.glfwPollEvents();
    }

    public void render() {
        swapBuffers();
    }

    public void swapBuffers() {
        GLFW.glfwSwapBuffers(window);
    }
}
