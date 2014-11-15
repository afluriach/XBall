package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.Controls;
import com.electricsunstudio.xball.FilterClass;

/**
 *
 * @author toni
 */
public class GoldBall extends Ball
{
	float radius = 0.5f;
	
	public GoldBall(MapObject mo)
	{
		super(mo);
		
		//load the appropriate sprite
		sprite = Game.loadSprite("gold_ball");
		
		physicsBody = Game.inst.physics.addCircleBody(
			Game.mapObjectPos(mo),
			radius,
			BodyType.DynamicBody,
			this,
			1,
			false,
			FilterClass.ball);
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
}
