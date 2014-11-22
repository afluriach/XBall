package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.physics.FilterClass;
import com.electricsunstudio.xball.physics.Physics;

/**
 *
 * @author toni
 */
public abstract class Ball extends GameObject
{
    float elasticity;
    float uk;
    float mass;
    String spriteName;
    
    public Ball(MapObject mo)
    {
        super(mo);
    }
    
    public Ball(String name)
    {
        super(name);
    }
    
    public Ball()
    {
        super("ball");
    }
    
    final void create(Vector2 pos)
    {
        //load the appropriate sprite
        sprite = Game.loadSprite(spriteName);
        
        physicsBody = Game.inst.physics.addCircleBody(
            pos,
            radius,
            BodyDef.BodyType.DynamicBody,
            this,
            mass,
            false,
            FilterClass.ball);
        
        Physics.setRestitution(physicsBody, elasticity);
    }

}
