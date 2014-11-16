package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.Controls;
import com.electricsunstudio.xball.FilterClass;
import java.util.ArrayList;

/**
 *
 * @author toni
 */
public class Player extends GameObject
{
	float speed = 3f;
	float accel = 5f;
	
	float radius = 0.5f;
	
	Sprite actionEffect;
	float fadeStart = 0.4f;
	
	float kickInterval = 0.7f;
	float kickDist = 1.5f;
	float kickWidth = 30f;
	float kickPower = 10f;
	
	float actionCooldown;
	
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
		actionCooldown -= Game.SECONDS_PER_FRAME;

		if(actionEffect != null && actionCooldown < fadeStart)
		{
			actionEffect.setAlpha(actionCooldown / fadeStart);
		}
		if(actionCooldown <= 0)
		{
			actionCooldown = 0;
			actionEffect = null;
		}
	}
	
	@Override
	public void render(SpriteBatch batch)
	{
		if(actionEffect != null)
		{
			Game.drawSprite(actionEffect, batch);
		}
		super.render(batch);
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
		handleMoveControls();
		handleActionControls();
	}
	
	public void handleMoveControls()
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
	
	public void handleActionControls()
	{
		Controls controls = Game.inst.controls;
		
		if(controls.kick && actionCooldown <= 0)
		{
			kick();
		}
	}
	
	ArrayList<GameObject> coneQuery(float radius, float angle, float halfwidthAngle)
	{
		//do an AABB query then constrain to circle and then cone
		Rectangle rect = new Rectangle();
		rect.width = radius/2;
		rect.height = radius/2;
		rect.setCenter(getCenterPos());
		
		ArrayList<GameObject> prospective = Game.inst.physics.getWithinSpace(rect);
		ArrayList<GameObject> actual = new ArrayList();
		
		double widthCos = Math.cos(Math.toRadians(halfwidthAngle));
		Vector2 facing = Game.rayRad(1, Math.toRadians(angle));
		
		for(GameObject go : prospective)
		{
			Vector2 disp = go.getCenterPos().sub(getCenterPos());
			if(disp.len2() <= radius*radius &&
			   disp.dot(facing) >= widthCos)
			{
				actual.add(go);
			}
		}
		return actual;
	}
	
	void kick()
	{
		actionCooldown = kickInterval;
		
		//show the kick effect, sprite will remain in place and fade out
		actionEffect = Game.loadSprite("kick_effect");
		//draw the kick effect in front of the player
		Vector2 disp = Game.rayRad(radius, Math.toRadians(rotation+90));
		actionEffect.setCenter((getCenterPos().x+disp.x)*Game.PIXELS_PER_TILE, (getCenterPos().y+disp.y)*Game.PIXELS_PER_TILE);
		actionEffect.setRotation(rotation);
		
		ArrayList<GameObject> targets = coneQuery(radius + kickDist, rotation+90, kickWidth);
		
		//any object in the cone will have a force applied
		for(GameObject go : targets)
		{
			//force propagates as a cone, i.e. the direction of impulse should
			//be the direction from the player to the object
			
			//TODO apply impulse at the point on the object where the line
			//touches the object. this will cause the object to move at more of
			//an angle when it hits
			
			//TODO kick power that drops of with distance, after say 75% of the
			//kick range
			
			Vector2 impulse = go.getCenterPos().sub(getCenterPos());
			impulse.nor().scl(kickPower);
			
			go.physicsBody.applyLinearImpulse(impulse, go.getCenterPos(), true);
		}
	}
}
