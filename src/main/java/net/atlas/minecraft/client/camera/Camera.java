package net.atlas.minecraft.client.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    private Vector3f position, orientation;

    public float pitch = 0;
    public float yaw = 0;

    public Camera(float x, float y, float z) {
        this.position = new Vector3f(x,y,z);
        this.orientation = new Vector3f(0,1,0);
    }

    public void translate(float x, float y, float z) {

        Vector3f offset = new Vector3f(x,y,z);
        offset.rotateY((float) Math.toRadians(yaw),offset);

        position.x += offset.x;
        position.y += offset.y;
        position.z += offset.z;
    }

    public void setLookDir(float mouseX, float mouseY) {
        yaw = mouseX;
        pitch = mouseY;

        if(yaw >= 90) yaw = 90;
        if(pitch >= 90) pitch = 90;
    }

    public Matrix4f getMatrix() {

        Vector3f lookPoint = new Vector3f(0,0,-1);

        lookPoint.rotateX((float)Math.toRadians(pitch), lookPoint);
        lookPoint.rotateY((float)Math.toRadians(yaw), lookPoint);

        lookPoint.add(position,lookPoint);

        Matrix4f matrix4f = new Matrix4f();
        matrix4f.lookAt(position,lookPoint,orientation,matrix4f);
        return matrix4f;
    }

}
