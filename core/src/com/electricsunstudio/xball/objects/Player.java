package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.Controls;
import com.electricsunstudio.xball.FilterClass;

/**
 *
 * @author toni
 */
public class Player extends GameObject
{
	float speed = 3f;
	float accel = 5f;
	
	float radius = 0.5f;
	
	public Player(MapObject mo)
	{
		super(mo);
		
		String color = mo.getProperties().get("color", String.class);
		
		//load the appropriate sprite
		sprite = Game.loadSprite(color+"_player");
		
		physicsBody = Game.inst.physics.addCircleBody(
			Game.mapObjectPos(mo),
			radius,
			BodyType.DynamicBody,
			this,
			10,
			false,
			FilterClass.player);
	}
	
	public void update()
	{
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

	public void handleControls()
	{
		Controls controls = Game.inst.controls;
		
		Vector2 targetVelocity = controls.controlPadPos.cpy().scl(speed);
		Vector2 velDisp = targetVelocity.cpy().sub(getVel());
		float dv = accel*Game.SECONDS_PER_FRAME;
		
		if(velDisp.len2() < dv*dv)
		{
			//the desired velocity can be achieved within one frame of acceleration
			//just set it directly
			setVel(targetVelocity);
			setAccel(null);
		}
		else
		{
			//scale velDisp to actual acceleration
			setAccel(velDisp.nor().scl(accel));
		}
		
		if(targetVelocity.len2() != 0)
		{
			setRotation(targetVelocity);
		}
	}
}
