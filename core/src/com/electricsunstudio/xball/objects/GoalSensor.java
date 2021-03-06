package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.BallNotify;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.Controls;
import com.electricsunstudio.xball.physics.FilterClass;

/**
 *
 * @author toni
 */
public class GoalSensor extends GameObject
{
    public BallNotify notifier;
    
    public GoalSensor(MapObject mo)
    {
        super(mo);

        physicsBody = Game.inst.physics.addRectBody(
            mo,
            BodyType.StaticBody,
            this,
            1f,
            true,
            FilterClass.ballSensor);
    }
    @Override
    public void update()
    {
    }
    
    @Override
    public void handleContact(GameObject other)
    {
        if(other instanceof GoldBall || other instanceof BlueBall)
        {
            Game.log(other.getName() + " arrived in " + getName());
            if(notifier != null)
            {
                notifier.onReceived((Ball)other);
            }
        }
    }
    public void handleEndContact(GameObject other)
    {
        
    }
    public void init()
    {
        
    }
}
