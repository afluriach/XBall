package com.electricsunstudio.xball;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GameObjectSystem
{
	ArrayList<GameObject> gameObjects = new ArrayList<GameObject>();
	Map<String, GameObject> nameMap = new TreeMap<String, GameObject>();
	ArrayList<GameObject> objectsToAdd = new ArrayList<GameObject>();
	
	public void clear()
	{
		gameObjects.clear();
		nameMap.clear();
		objectsToAdd.clear();
	}
	
	public GameObjectSystem()
	{
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
	
	public void handleAdditions()
	{
		for(GameObject go : objectsToAdd)
		{
			gameObjects.add(go);
			if(go.name != null) nameMap.put(go.name, go);
		}
		objectsToAdd.clear();
	}
	
	private void remove(GameObject go)
	{
		gameObjects.remove(go);
		nameMap.remove(go.name);		
	}

	
	public void updateAll()
	{
		for(GameObject go : gameObjects)
		{
			go.update();
		}
	}
	
	public void initAll()
	{
		for(GameObject go : gameObjects)
		{
			go.init();
		}
	}
	
	public void removeExpired()
	{
		ArrayList<GameObject> expired = new ArrayList<GameObject>();
		
		for(GameObject go : gameObjects)
		{
			if(go.expired)
			{
				expired.add(go);
			}
		}
		
		//if a gameobject is touching another when it is expiring, the physics engine will not register an end contact
		for(GameObject go : expired)
		{
			if(go.physicsBody != null)
			{
				Game.inst.physics.removeBody(go.physicsBody);
			}
			remove(go);
		}
	}
		
	public void render(SpriteBatch sb)
	{
		for(GameObject go : gameObjects)
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
		ArrayList<GameObject> results = new ArrayList<GameObject>();
		for(GameObject go : gameObjects)
		{
			if(cls.isInstance(go))
				results.add(go);
		}
		return (List<T>) results;
	}
    
	public <T> T getObjectByType(Class<T> cls)
	{
		ArrayList<GameObject> results = new ArrayList<GameObject>();
		for(GameObject go : gameObjects)
		{
			if(cls.isInstance(go))
				return (T) go;
		}
		return null;
	}
	
	public int countObjectsByType(Class cls)
	{
		int count = 0;
		for(GameObject go : gameObjects)
		{
			if(cls.isInstance(go))
				++count;
		}
		return count;		
	}
	
	public List<GameObject> getObjects()
	{
		return gameObjects;
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
		for(GameObject go : gameObjects)
		{
			go.applyAccel();
		}
	}
}
