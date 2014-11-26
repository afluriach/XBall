package com.electricsunstudio.xball;

import com.badlogic.gdx.math.Vector2;
import java.io.Serializable;


public class GameObjectState implements Serializable
{
    private static final long serialVersionUID = -985743503464740897L;
    public String name;
    public int uid;
    public float posX,posY;
    public float velX,velY;
    public float angularPos;
    public float angularVel;

    public GameObjectState(GameObject go)
    {
        name = go.getName();
        uid = go.getUid();

        Vector2 pos = go.getCenterPos();
        Vector2 vel = go.getVel();

        posX = pos.x;
        posY = pos.y;

        velX = vel.x;
        velY = vel.y;

        angularPos = go.getRotationRad();
        angularVel = go.physicsBody.getAngularVelocity();
    }

    public void applyState(GameObject go)
    {
        if(!name.equals(go.getName()))
            System.out.println("GameObject state name doesn't match object name");
        if(uid != go.getUid())
            System.out.println("GameObject state UID doesn't match object UID");

        go.setPos(new Vector2(posX, posY));
        go.setVel(new Vector2(velX,velY));

        go.setRotationRad(angularPos);
        go.physicsBody.setAngularVelocity(angularVel);
    }        
}
