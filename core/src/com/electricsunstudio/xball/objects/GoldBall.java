package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.Controls;
import com.electricsunstudio.xball.FilterClass;
import com.electricsunstudio.xball.Physics;

/**
 *
 * @author toni
 */
public class GoldBall extends Ball
{
	float radius = 0.5f;
	float elasticity = 0.4f;
	float uk = 0.02f;
	
	public GoldBall(MapObject mo)
	{
		super(mo);
		create(Game.mapObjectPos(mo));
	}
	
	public GoldBall(Vector2 pos)
	{
		super();
		create(pos);
	}
	
	void create(Vector2 pos)
	{
		//load the appropriate sprite
		sprite = Game.loadSprite("gold_ball");
		
		physicsBody = Game.inst.physics.addCircleBody(
			pos,
			radius,
			BodyType.DynamicBody,
			this,
			1,
			false,
			FilterClass.ball);
		
		Physics.setRestitution(physicsBody, elasticity);
	}
	
	public void update()
	{
		applyKineticFriction(uk);
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
