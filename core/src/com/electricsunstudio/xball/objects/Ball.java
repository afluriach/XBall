package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.electricsunstudio.xball.GameObject;

/**
 *
 * @author toni
 */
public abstract class Ball extends GameObject
{
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
}
