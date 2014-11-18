package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.Controls;
import com.electricsunstudio.xball.physics.FilterClass;
import com.electricsunstudio.xball.physics.Physics;

/**
 *
 * @author toni
 */
public class GoldBall extends Ball
{
	public GoldBall(MapObject mo)
	{
		super(mo);
		initFields();
		create(Game.mapObjectPos(mo));
	}
	
	public GoldBall(Vector2 pos)
	{
		super();
		initFields();
		create(pos);
	}
	
	final void initFields()
	{
		radius = 0.35f;
		elasticity = 0.4f;
		uk = 0.02f;
		mass = 1;
		spriteName = "gold_ball";
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
