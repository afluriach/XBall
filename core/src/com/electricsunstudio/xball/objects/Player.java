package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Joint;
import com.electricsunstudio.xball.Action;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.Controls;
import com.electricsunstudio.xball.physics.FilterClass;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author toni
 */
public class Player extends GameObject
{
	float speed = 3f;
	float accel = 5f;
	float angularSpeedMult = 10f;
	
	float roughSpeedPenalty = 0.7f;
	
	Sprite actionEffect;
	float fadeStart = 0.4f;
	
	float kickInterval = 0.7f;
	float kickDist = 1.75f;
	float kickWidth = 30f;
	float kickPower = 10f;

	float grabDist = 1.5f;
	float grabWidth = 30f;

	float actionCooldown;
	
	ArrayList<Joint> grabJoints = new ArrayList();
	boolean grabbing = false;
	
	public Player(MapObject mo)
	{
		super(mo);
		
		radius = 0.5f;
		
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

		if(actionEffect != null && !grabbing && actionCooldown < fadeStart)
		{
			actionEffect.setAlpha(actionCooldown / fadeStart);
		}
		if(actionCooldown <= 0 && !grabbing)
		{
			actionCooldown = 0;
			actionEffect = null;
		}
		
		if(grabbing && actionEffect != null)
		{
			Vector2 disp = Game.rayRad(radius+actionEffect.getHeight()/2*Game.TILES_PER_PIXEL, Math.toRadians(getRotation()));
			actionEffect.setCenter((getCenterPos().x+disp.x)*Game.PIXELS_PER_TILE, (getCenterPos().y+disp.y)*Game.PIXELS_PER_TILE);
			actionEffect.setRotation(getRotation()-90);
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
		handleAimControls();
		handleActionControls();
	}
	
	public void handleMoveControls()
	{
		Controls controls = Game.inst.controls;
		float actualAccel = accel;
		if(Game.inst.onMapLayer("ice", getCenterPos()))
			actualAccel *= iceTraction;
		
		float actualSpeed = speed;
		if(Game.inst.onMapLayer("rough", getCenterPos()))
			actualSpeed *= roughSpeedPenalty;
		
		Vector2 targetVelocity = controls.controlPadPos.cpy().scl(actualSpeed);
		Vector2 velDisp = targetVelocity.cpy().sub(getVel());
		float dv = actualAccel*Game.SECONDS_PER_FRAME;
		
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
			setAccel(velDisp.nor().scl(actualAccel));
		}
		
	}
	
	void handleAimControls()
	{
		if(Game.inst.controls.aimPadPos.len2() != 0)
		{
			float targetAngle = Game.inst.controls.aimPadPos.angleRad();
			
			//predict 1/2 second ahead
			float nextAngle = getRotationRad() + physicsBody.getAngularVelocity() / 2;
			float totalRotation = targetAngle- nextAngle;
			while ( totalRotation < -Math.PI ) totalRotation += Math.PI*2;
			while ( totalRotation >  Math.PI) totalRotation -= Math.PI*2;
			float desiredAngularVelocity = totalRotation*angularSpeedMult;
			float impulse = physicsBody.getInertia() * desiredAngularVelocity*Game.SECONDS_PER_FRAME;
			physicsBody.applyAngularImpulse(impulse, true);
		}
	}
	
	public void handleActionControls()
	{
		Controls controls = Game.inst.controls;
		
		if(controls.state.get(Action.kick) && actionCooldown <= 0 && !grabbing)
		{
			kick();
		}
		
		if(!grabbing && controls.state.get(Action.grab))
		{
			grabStart();
			grabbing = true;
		}
		else if(grabbing && !controls.state.get(Action.grab))
		{
			grabEnd();
			grabbing = false;
		}
	}
	
	ArrayList<GameObject> coneQuery(float radius, float angle, float halfwidthAngle)
	{
		//do an AABB query then constrain to circle and then cone
		Rectangle rect = new Rectangle();
		rect.width = radius*2;
		rect.height = radius*2;
		rect.setCenter(getCenterPos());
		
		ArrayList<GameObject> prospective = Game.inst.physics.getWithinSpace(rect);
		ArrayList<GameObject> actual = new ArrayList();
		
		double widthCos = Math.cos(Math.toRadians(halfwidthAngle));
		Vector2 facing = Game.rayRad(1, Math.toRadians(angle));
		
		for(GameObject go : prospective)
		{
			Vector2 disp = go.getCenterPos().sub(getCenterPos());
			if(disp.len2() <= (radius+go.radius)*(radius+go.radius) &&
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
		Vector2 disp = Game.rayRad(radius+actionEffect.getHeight()/2*Game.TILES_PER_PIXEL, Math.toRadians(getRotation()));
		actionEffect.setCenter((getCenterPos().x+disp.x)*Game.PIXELS_PER_TILE, (getCenterPos().y+disp.y)*Game.PIXELS_PER_TILE);
		actionEffect.setRotation(getRotation()-90);
		
		ArrayList<GameObject> targets = coneQuery(radius + kickDist, getRotation(), kickWidth);
		
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
	
	void grabStart()
	{
		//show the grab effect, sprite will remain in place while grabbing
		actionEffect = Game.loadSprite("grab_effect");
		//draw the grab effect in front of the player
		Vector2 disp = Game.rayRad(radius+actionEffect.getHeight()/2*Game.TILES_PER_PIXEL, Math.toRadians(getRotation()));
		actionEffect.setCenter((getCenterPos().x+disp.x)*Game.PIXELS_PER_TILE, (getCenterPos().y+disp.y)*Game.PIXELS_PER_TILE);
		actionEffect.setRotation(getRotation()-90);
		
		ArrayList<GameObject> targets = coneQuery(radius + 0.25f + grabDist, getRotation(), grabWidth);
		
		for(GameObject go : targets)
		{
			//grab object by joining to it
			List<Joint> joints = Game.inst.physics.joinBodies(physicsBody, go.physicsBody);
			grabJoints.addAll(joints);
		}
	}
	
	void grabEnd()
	{
		Game.log("grab end");
		for(Joint j : grabJoints)
		{
			Game.log("trying to remove");
			Game.inst.physics.removeJoint(j);
		}
		grabJoints.clear();
		actionEffect = null;
	}
}
