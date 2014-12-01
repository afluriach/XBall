package com.electricsunstudio.xball;

import com.electricsunstudio.xball.GameObject;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;

public class GameObjectSystemState implements Serializable
{
    private static final long serialVersionUID = 7891023312807738439L;
    GameObjectState[] states;

    public GameObjectSystemState(GameObjectSystem gos)
    {
        ArrayList<GameObjectState> stateList = new ArrayList<GameObjectState>();

        for(GameObject go : gos.gameObjects.values())
        {
            if(!gos.stateIgnore.contains(go.getClass())){
                stateList.add(go.getState());
                //System.out.println("added state of type " + stateList.get(stateList.size()-1).getClass().getName());
            }
        }
        states = stateList.toArray(new GameObjectState[0]);
    }

    public void applyState(GameObjectSystem gos)
    {
        //we only need to restore state of already existant objects
        HashSet<GameObjectState> updates = new HashSet<GameObjectState>(states.length);
        //if we didn't see a state for an object, it is no longer present
        HashSet<Integer> objsToDelete = new HashSet<Integer>();            
        objsToDelete.addAll(gos.gameObjects.keySet());

        for(GameObjectState s : states)
        {
            if(!gos.gameObjects.containsKey(s.uid))
            {
                //instantiate not currently present objects                    
                //map object state class to the concerete class that needs
                //to be instantiated
                gos.addObject(gos.createObjectFromState(s));
            }
            else
            {
                //state will be updated
                updates.add(s);
                //object is still present
                objsToDelete.remove(s.uid);
            }
        }

        //remove no longer existant object
        //except if an object is in state ignore. we do not expect an update
        //and it will not be deleted
        for(Integer id : objsToDelete)
        {
            if(!gos.stateIgnore.contains(gos.gameObjects.get(id).getClass()))
            {
                GameObject go = gos.gameObjects.get(id);
                if(go.physicsBody != null)
                    Game.inst.physics.removeBody(go.physicsBody);
                gos.remove(go);
            }
        }

        gos.handleAdditions();

        //apply state to existant objects
        for(GameObjectState s : updates)
        {
            gos.gameObjects.get(s.uid).restoreFromState(s);
        }
    }
}
