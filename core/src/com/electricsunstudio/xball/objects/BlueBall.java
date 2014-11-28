package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.GameObjectState;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.Controls;
import com.electricsunstudio.xball.physics.FilterClass;
import com.electricsunstudio.xball.physics.Physics;

/**
 *
 * @author toni
 */
public class BlueBall extends Ball
{
    public BlueBall(MapObject mo)
    {
        super(mo);
        initFields();
        create(Game.mapObjectPos(mo));
    }
    
    public BlueBall(Vector2 pos)
    {
        super();
        initFields();
        create(pos);
    }
    
    public BlueBall(GoldBallState s)
    {
        super(s.name);
        initFields();
        create(new Vector2(s.posX, s.posY));
        restoreFromState(s);
    }
    
    final void initFields()
    {
        radius = 0.35f;
        elasticity = 0.4f;
        uk = 0.02f;
        mass = 1;
        spriteName = "blue_ball";
    }

    public void update()
    {
        applyKineticFriction(uk);
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
    
    @Override
    public GameObjectState getState()
    {
        return new BlueBallState(this);
    }
}
