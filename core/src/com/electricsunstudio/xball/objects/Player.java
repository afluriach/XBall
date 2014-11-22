package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Joint;
import com.electricsunstudio.xball.Action;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.ControlState;
import com.electricsunstudio.xball.physics.FilterClass;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author toni
 */
public class Player extends GameObject
{
    float speed = 3f;
    float accel = 5f;
    float angularSpeedMult = 10f;
    
    float roughSpeedPenalty = 0.7f;
    
    Sprite actionEffect;
    float fadeStart = 0.4f;
    
    float kickInterval = 0.7f;
    float kickDist = 1.75f;
    float kickWidth = 30f;
    float kickPower = 10f;

    float grabDist = 1.5f;
    float grabWidth = 30f;
    float grabVelCorrectionForce = 7f;
    float grabPosCorrectionForce = 14f;
    float dropLimit = 0.75f;
    float grabInterval  = 0.7f;
    float minGrabTime = 0.125f;

    float actionCooldown;
    float grabTime;
    
    ArrayList<GameObject> grabbedObjects = new ArrayList();
    ArrayList<Vector2> grabbedOffsets = new ArrayList();
    boolean grabbing = false;
    
    public Player(MapObject mo)
    {
        super(mo);
        
        radius = 0.5f;
        
        String color = mo.getProperties().get("color", String.class);
        
        //load the appropriate sprite
        sprite = Game.loadSprite(color+"_player");
        
        physicsBody = Game.inst.physics.addCircleBody(
            Game.mapObjectPos(mo),
            radius,
            BodyType.DynamicBody,
            this,
            10,
            false,
            FilterClass.player);
    }
    
    public void update()
    {
        actionCooldown -= Game.SECONDS_PER_FRAME;

        if(actionEffect != null && !grabbing && actionCooldown < fadeStart)
        {
            actionEffect.setAlpha(actionCooldown / fadeStart);
        }
        if(actionCooldown <= 0 && !grabbing)
        {
            actionCooldown = 0;
            actionEffect = null;
        }

        if(grabbing && actionEffect != null)
        {
            Vector2 disp = Game.rayRad(radius+actionEffect.getHeight()/2*Game.TILES_PER_PIXEL, Math.toRadians(getRotation()));
            actionEffect.setCenter((getCenterPos().x+disp.x)*Game.PIXELS_PER_TILE, (getCenterPos().y+disp.y)*Game.PIXELS_PER_TILE);
            actionEffect.setRotation(getRotation()-90);
        }
        
        //apply impulse to try to move objects with the player
        if(grabbing)
        {
            grabTime += Game.SECONDS_PER_FRAME;
            
            for(int i=0;i<grabbedObjects.size(); ++i)
            {
                GameObject go = grabbedObjects.get(i);
                Vector2 offset = grabbedOffsets.get(i).cpy().rotate(getRotation());
                Vector2 desiredPos = getCenterPos().add(offset);
                Vector2 disp = desiredPos.sub(go.getCenterPos());

                //if object expires or moves more than the limit from its original
                //grab offset, drop it
                if(go.isExpired() || disp.len2() >= dropLimit*dropLimit)
                {
                    grabbedObjects.remove(i);
                    grabbedOffsets.remove(i);
                    --i;
                    continue;
                }
                
                Vector2 dv = getVel().sub(go.getVel());
                Vector2 linearForce = dv.scl(go.physicsBody.getMass()*grabVelCorrectionForce);
                //and a bit more to push it towards the desired point
                //not a uniform force, it is scaled by distance
                linearForce.add(disp.scl(grabPosCorrectionForce));
                go.physicsBody.applyLinearImpulse(linearForce.scl(Game.SECONDS_PER_FRAME), go.getCenterPos(), true);
            }
        }
    }
    
    @Override
    public void render(SpriteBatch batch)
    {
        if(actionEffect != null)
        {
            Game.drawSprite(actionEffect, batch);
        }
        super.render(batch);
    }
    
    public void handleContact(GameObject other)
    {
        
    }
    public void handleEndContact(GameObject other)
    {
        
    }
    public void init()
    {
        
    }

    public void handleControls(ControlState state)
    {
        handleMoveControls(state);
        handleActionControls(state);
    }
    
    public void handleMoveControls(ControlState state)
    {
        float actualAccel = accel;
        if(Game.inst.onMapLayer("ice", getCenterPos()))
            actualAccel *= iceTraction;
        
        float actualSpeed = speed;
        if(Game.inst.onMapLayer("rough", getCenterPos()))
            actualSpeed *= roughSpeedPenalty;
        
        Vector2 movePos = new Vector2(state.moveX, state.moveY);
        
        Vector2 targetVelocity = movePos.cpy().scl(actualSpeed);
        Vector2 velDisp = targetVelocity.cpy().sub(getVel());
        float dv = actualAccel*Game.SECONDS_PER_FRAME;
        
        if(velDisp.len2() < dv*dv)
        {
            //the desired velocity can be achieved within one frame of acceleration
            //just set it directly
            setVel(targetVelocity);
            setAccel(null);
        }
        else
        {
            //scale velDisp to actual acceleration
            setAccel(velDisp.nor().scl(actualAccel));
        }
        
        if(movePos.len2() != 0 && !state.grab && !state.lock)
        {
            setRotation(movePos);
            physicsBody.setAngularVelocity(0f);
        }
        else if(state.grab || state.lock)
            physicsBody.setAngularVelocity(0f);
    }
    
    public void handleActionControls(ControlState state)
    {
        if(state.kick && actionCooldown <= 0 && !grabbing)
        {
            kick();
        }
        
        if(!grabbing && state.grab && actionCooldown <= 0)
        {
            grabStart();
            grabbing = true;
        }
        else if(grabbing && grabTime > minGrabTime && (!state.grab || grabbedObjects.isEmpty()))
        {
            grabEnd();
            grabbing = false;
        }
    }
    
    ArrayList<GameObject> coneQuery(float radius, float angle, float halfwidthAngle)
    {
        //do an AABB query then constrain to circle and then cone
        Rectangle rect = new Rectangle();
        rect.width = radius*2;
        rect.height = radius*2;
        rect.setCenter(getCenterPos());
        
        ArrayList<GameObject> prospective = Game.inst.physics.getWithinSpace(rect);
        ArrayList<GameObject> actual = new ArrayList();
        
        double widthCos = Math.cos(Math.toRadians(halfwidthAngle));
        Vector2 facing = Game.rayRad(1, Math.toRadians(angle));
        
        for(GameObject go : prospective)
        {
            Vector2 disp = go.getCenterPos().sub(getCenterPos());
            if(disp.len2() <= (radius+go.radius)*(radius+go.radius) &&
               disp.dot(facing) >= widthCos)
            {
                actual.add(go);
            }
        }
        return actual;
    }

    void kick()
    {
        actionCooldown = kickInterval;
        
        //show the kick effect, sprite will remain in place and fade out
        actionEffect = Game.loadSprite("kick_effect");
        //draw the kick effect in front of the player
        Vector2 disp = Game.rayRad(radius+actionEffect.getHeight()/2*Game.TILES_PER_PIXEL, Math.toRadians(getRotation()));
        actionEffect.setCenter((getCenterPos().x+disp.x)*Game.PIXELS_PER_TILE, (getCenterPos().y+disp.y)*Game.PIXELS_PER_TILE);
        actionEffect.setRotation(getRotation()-90);
        
        ArrayList<GameObject> targets = coneQuery(radius + kickDist, getRotation(), kickWidth);
        
        //any object in the cone will have a force applied
        for(GameObject go : targets)
        {
            //force propagates as a cone, i.e. the direction of impulse should
            //be the direction from the player to the object
            
            //TODO apply impulse at the point on the object where the line
            //touches the object. this will cause the object to move at more of
            //an angle when it hits
            
            //TODO kick power that drops of with distance, after say 75% of the
            //kick range
            
            Vector2 impulse = go.getCenterPos().sub(getCenterPos());
            impulse.nor().scl(kickPower);
            
            go.physicsBody.applyLinearImpulse(impulse, go.getCenterPos(), true);
        }
    }
    
    void grabStart()
    {
        grabTime = 0f;
        //show the grab effect, sprite will remain in place while grabbing
        actionEffect = Game.loadSprite("grab_effect");
        //draw the grab effect in front of the player
        Vector2 disp = Game.rayRad(radius+actionEffect.getHeight()/2*Game.TILES_PER_PIXEL, Math.toRadians(getRotation()));
        actionEffect.setCenter((getCenterPos().x+disp.x)*Game.PIXELS_PER_TILE, (getCenterPos().y+disp.y)*Game.PIXELS_PER_TILE);
        actionEffect.setRotation(getRotation()-90);
        
        ArrayList<GameObject> targets = coneQuery(radius + 0.25f + grabDist, getRotation(), grabWidth);
        
        for(GameObject go : targets)
        {
            if(!(go instanceof Ball)) continue;
            
            Game.log(go.getName() + " grabbed at " + Game.string(go.getCenterPos()));
            grabbedObjects.add(go);
            //rotate offset to account for facing direction
            grabbedOffsets.add(go.getCenterPos().sub(getCenterPos()).rotate(-getRotation()));
        }
    }
    
    void grabEnd()
    {
        Game.log("grab end");
        grabbedObjects.clear();
        grabbedOffsets.clear();
        actionEffect = null;
        actionCooldown = grabInterval;
    }
}
