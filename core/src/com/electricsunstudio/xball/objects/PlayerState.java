package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.GameObjectState;
import com.electricsunstudio.xball.EffectSprite;
import java.io.Serializable;
import java.util.ArrayList;

public class PlayerState extends GameObjectState
{
    private static final long serialVersionUID = -3660870950896580940L;
    public float actionCooldown;
    public float grabTime;
    public boolean grabbing;
    ArrayList<Integer> grabbedObjects;
    ArrayList<Vector2> grabbedOffsets;
    
    EffectSprite effect;

    public PlayerState(Player p)
    {
        super(p);
        actionCooldown = p.actionCooldown;
        grabTime = p.grabTime;
        grabbing = p.grabbing;
        effect = p.actionEffect;

        if(grabbing)
        {
            //for serialization: this list cant change once created, so it
            //the serializer should not pass it by identity on repeated writes
            grabbedOffsets = (ArrayList<Vector2>) p.grabbedOffsets.clone();
            grabbedObjects = new ArrayList<Integer>(p.grabbedObjects.size());

            for(GameObject go : p.grabbedObjects)
            {
                grabbedObjects.add(go.getUid());
            }

            p.grabbedObjects = new ArrayList<GameObject>(grabbedObjects.size());

            for(Integer objId : grabbedObjects)
            {
                p.grabbedObjects.add(Game.inst.gameObjectSystem.getByUid(objId));
            }
        }
        if(p.actionEffect != null)
        {
        }
    }

    public void applyState(Player p)
    {
        super.applyState(p);
        p.actionCooldown = actionCooldown;
        p.grabTime = grabTime;
        p.grabbing = grabbing;

        if(p.actionEffect != null)
        {
            p.actionEffect.getSprite().getTexture().dispose();
        }
        
        p.actionEffect = effect;
        if(p.actionEffect != null)
            p.actionEffect.recreateSprite();
        
        if(grabbing)
        {
            p.grabbedOffsets = grabbedOffsets;
            p.grabbedObjects = new ArrayList<GameObject>(grabbedObjects.size());
            
            for(Integer id : grabbedObjects)
            {
                p.grabbedObjects.add(Game.inst.gameObjectSystem.getByUid(id));
            }
        }
    }
}
