package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.FilterClass;

/**
 *
 * @author toni
 */
public class Player extends GameObject
{
	public Player(MapObject mo)
	{
		super(mo);
		
		String color = mo.getProperties().get("color", String.class);
		
		//load the appropriate sprite
		sprite = Game.loadSprite(color+"_player");
		
		physicsBody = Game.inst.physics.addCircleBody(
			Game.mapObjectPos(mo),
			1,
			BodyType.DynamicBody,
			this,
			10,
			false,
			FilterClass.player);
	}
	
	public void update()
	{
		rotation += 1;
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

}
