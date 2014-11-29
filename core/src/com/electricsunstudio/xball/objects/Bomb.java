package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.GameObjectState;
import com.electricsunstudio.xball.HitListener;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.HitType;
import java.util.ArrayList;

/**
 *
 * @author toni
 */
public class Bomb extends Ball
{
    static final float fuseTime = 5f;
    static final float partialTime = 3f;
    static final float imminentTime = 1.5f;
    static final float baseForce = 500f;
    //the more realistic option would be to apply an impulse every frame for the
    //duration of the blast
    static final float blastTime = 0.1f;
    static final float blastRadius = 7f;
    //player will be considered "hit" if within this radius
    static final float hitRadius = 4f;
    
    float fuseRemaining = fuseTime;
    SpriteState spriteState = SpriteState.normal;
    
    enum SpriteState{
        normal,
        partial,
        imminent
    }
    
    public Bomb(MapObject mo)
    {
        super(mo);
        initFields();
        create(Game.mapObjectPos(mo));
    }
    
    public Bomb(Vector2 pos)
    {
        super();
        initFields();
        create(pos);
    }
    
    public Bomb(BombState s)
    {
        super(s.name);
        initFields();
        create(new Vector2(s.posX, s.posY));
        restoreFromState(s);
    }
    
    final void initFields()
    {
        radius = 0.35f;
        elasticity = 0.2f;
        uk = 0.02f;
        mass = 3;
        spriteName = "bomb";
    }
    
    @Override
    public void update() {
        fuseRemaining -= Game.SECONDS_PER_FRAME;
        
        if(spriteState == SpriteState.normal && fuseRemaining < partialTime)
        {
            Game.changeTexture(sprite, "bomb_partial");
            spriteState = SpriteState.partial;
        }
        else if(spriteState == SpriteState.partial && fuseRemaining < imminentTime)
        {
            Game.changeTexture(sprite, "bomb_imminent");
            spriteState = SpriteState.imminent;
        }
        
        if(fuseRemaining <= 0)
        {
            detonate();
            expire();
        }
    }

    //apply base force to any object at 1 meter; otherwise, apply force that
    //drops off quadratically with distance
    void detonate()
    {
        ArrayList<GameObject> targets = circleQuery(blastRadius);
        
        for(GameObject target : targets)
        {
            Vector2 disp = target.getCenterPos().sub(getCenterPos());
            
            float actualForce = baseForce;
            if(target instanceof Ball)
                actualForce *= 0.7f;
            
            if(disp.len2() <= blastRadius*blastRadius)
            {
                //apply full force within the core blast radius
                Vector2 impulse = disp.nor().scl(actualForce*blastTime);
                target.physicsBody.applyLinearImpulse(impulse, target.getCenterPos(), true);
                
                //and notify player by bomb if level is a HitListener
                if(Game.inst.crntLevel instanceof HitListener && target instanceof Player)
                {
                    ((HitListener)Game.inst.crntLevel).onHit((Player)target, HitType.bomb);
                }
            }
            else
            {
                float dist = disp.len();
                //scale force according to distance from the core radius
                float forceMag = actualForce / (dist-blastRadius)/(dist-blastRadius);
                Vector2 impulse = disp.nor().scl(forceMag*blastTime);
                target.physicsBody.applyLinearImpulse(impulse, target.getCenterPos(), true);
            }           
        }
    }
    
    ArrayList<GameObject> circleQuery(float radius)
    {
        //do an AABB query then constrain to circle
        Rectangle rect = new Rectangle();
        rect.width = radius*2;
        rect.height = radius*2;
        rect.setCenter(getCenterPos());
        
        ArrayList<GameObject> prospective = Game.inst.physics.getWithinSpace(rect);
        ArrayList<GameObject> actual = new ArrayList();
        
        for(GameObject go : prospective)
        {
            Vector2 disp = go.getCenterPos().sub(getCenterPos());
            if(disp.len2() <= (radius+go.radius)*(radius+go.radius))
            {
                actual.add(go);
            }
        }
        return actual;
    }
    
    @Override
    public void handleContact(GameObject other) {
    }

    @Override
    public void handleEndContact(GameObject other) {
    }

    @Override
    public void init() {
    }
    
    @Override
    public GameObjectState getState()
    {
        return new BombState(this);
    }
    
    @Override
    public void restoreFromState(GameObjectState s)
    {
        super.restoreFromState(s);
        ((BombState)s).applyState(this);
    }
}
