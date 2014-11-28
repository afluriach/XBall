package com.electricsunstudio.xball;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.electricsunstudio.xball.objects.Bomb;
import com.electricsunstudio.xball.objects.BombState;
import com.electricsunstudio.xball.objects.GoalSensor;
import com.electricsunstudio.xball.objects.GoldBall;
import com.electricsunstudio.xball.objects.BlueBall;
import com.electricsunstudio.xball.objects.GoldBallState;
import com.electricsunstudio.xball.objects.BlueBallState;
import com.electricsunstudio.xball.objects.Player;
import com.electricsunstudio.xball.objects.PlayerState;
import com.electricsunstudio.xball.objects.SpawnSensor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GameObjectSystem
{
    HashMap<Integer, GameObject> gameObjects = new HashMap<Integer,GameObject>();
    Map<String, GameObject> nameMap = new TreeMap<String, GameObject>();
    ArrayList<GameObject> objectsToAdd = new ArrayList<GameObject>();
    HashMap<Class, ArrayList<GameObject>> typeMap = new HashMap<Class, ArrayList<GameObject>>();
    HashMap<Class,Class> stateToObjType = new HashMap<Class, Class>();
    HashSet<Class> stateIgnore = new HashSet<Class>();
    
    public void clear()
    {
        gameObjects.clear();
        nameMap.clear();
        objectsToAdd.clear();
    }
    
    public GameObjectSystem()
    {
        stateToObjType.put(BombState.class, Bomb.class);
        stateToObjType.put(GoldBallState.class, GoldBall.class);
        stateToObjType.put(BlueBallState.class, BlueBall.class);
        stateToObjType.put(PlayerState.class, Player.class);
        
        stateIgnore.add(SpawnSensor.class);
        stateIgnore.add(GoalSensor.class);
    }
    
    GameObject createObjectFromState(GameObjectState s)
    {
        Class objCls = stateToObjType.get(s.getClass());
        Constructor cons = null;

        try {
            cons = objCls.getConstructor(s.getClass());
        } catch (NoSuchMethodException ex) {
            System.out.printf("class %s lacks state constructor\n", objCls.getSimpleName());
        } catch (SecurityException ex) {
            System.out.printf("class %s, unable to call state constructor\n", objCls.getSimpleName());
        }

        if(cons == null)
        {
            System.out.println("unable to instantiate object of type " + objCls.getSimpleName());
        }

        try {
            return (GameObject) cons.newInstance(s);
        } catch (Exception ex) {
            System.out.println("exception creating class from state: " + objCls.getSimpleName());
            ex.printStackTrace();
        }
        return null;
    }

        
    /**
     * Adds GameObject before the next update cycle. Additions are not handled within the update cycle,
     * as this creates concurrent modification problems.
     * @param go
     */
    public void addObject(GameObject go)
    {
        objectsToAdd.add(go);
    }
    
    public void addAllObjects(Collection<GameObject> gameObjects)
    {
        objectsToAdd.addAll(gameObjects);
    }
    
    void addByType(GameObject go)
    {
        //add object to the type map
        Class cls = go.getClass();
        
        while(cls != GameObject.class)
        {
            if(!typeMap.containsKey(cls))
            {
                typeMap.put(cls, new ArrayList());
            }
            typeMap.get(cls).add(go);
            
            cls = cls.getSuperclass();
        }
    }
    
    void removeByType(GameObject go)
    {
        Class cls = go.getClass();
        
        while(cls != GameObject.class)
        {
            typeMap.get(cls).remove(go);
            
            cls = cls.getSuperclass();
        }
    }
    
    public void handleAdditions()
    {
        for(GameObject go : objectsToAdd)
        {
            gameObjects.put(go.uid, go);
            if(go.name != null) nameMap.put(go.name, go);
            addByType(go);
        }
        objectsToAdd.clear();
    }
    
    void remove(GameObject go)
    {
        gameObjects.remove(go.uid);
        nameMap.remove(go.name);
        removeByType(go);
    }

    
    public void updateAll()
    {
        for(GameObject go : gameObjects.values())
        {
            go.update();
        }
    }
    
    public void initAll()
    {
        for(GameObject go : gameObjects.values())
        {
            go.init();
        }
    }
    
    public void removeExpired()
    {
        ArrayList<GameObject> expired = new ArrayList<GameObject>();
        
        for(GameObject go : gameObjects.values())
        {
            if(go.expired)
            {
                expired.add(go);
            }
        }
        
        //if a gameobject is touching another when it is expiring, the physics engine will not register an end contact
        for(GameObject go : expired)
        {
            go.onExpire();
            remove(go);
        }
    }
        
    public void render(SpriteBatch sb)
    {
        for(GameObject go : gameObjects.values())
        {
            go.render(sb);
        }
    }
        
    //check if the object is still in existence and not expired
    public boolean hasObject(String name)
    {
        if(!nameMap.containsKey(name))
            return false;
        
        return !nameMap.get(name).isExpired();
    }

    
    public GameObject getObjectByName(String name)
    {
        if(!nameMap.containsKey(name))
            throw new RuntimeException(String.format("object %s not found", name));
        
        return nameMap.get(name);
    }
    
    //get object by name but also cast it to the desired type
    public <T> T getObjectByName(String name, Class<T> cls)
    {
        return (T) getObjectByName(name);
    }
    
    public <T> List<T> getObjectsByType(Class<T> cls)
    {
        if(!typeMap.containsKey(cls))
            return new LinkedList();
        
        return (List<T>) typeMap.get(cls);
    }
    
    public <T> T getObjectByType(Class<T> cls)
    {
        if(!typeMap.containsKey(cls) || typeMap.get(cls).isEmpty())
            return null;
        
        return (T) typeMap.get(cls).get(0);
    }
    
    public int countObjectsByType(Class cls)
    {
        if(!typeMap.containsKey(cls))
            return 0;
        
        return typeMap.get(cls).size();
    }
    
    public GameObject getByUid(int uid)
    {
        if(!gameObjects.containsKey(uid)) return null;
        return gameObjects.get(uid);
    }
    
    public Collection<GameObject> getObjects()
    {
        return gameObjects.values();
    }
    
    public boolean allExpired(String[] names)
    {
        for(String name : names)
        {
            if (hasObject(name)) return false;
        }       
        return true;
    }
    
    public void update()
    {
        handleAdditions();
        updateAll();
        removeExpired();
    }
    
    public void applyAccel()
    {
        for(GameObject go : gameObjects.values())
        {
            go.applyAccel();
        }
    }
    
    public GameObjectSystemState getState()
    {
        return new GameObjectSystemState(this);
    }
    
    public void restoreFromState(GameObjectSystemState s)
    {
        s.applyState(this);
    }
}
