package com.electricsunstudio.xball;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import java.util.Map;
import java.util.TreeMap;
import com.badlogic.gdx.physics.box2d.QueryCallback;


/**
 *
 * @author toni
 */
public class Physics {
	World world;

	public Physics()
	{
		//no gravity, allow sleeping objects
		world = new World(new Vector2(0,0), true);
	}
	
	public Body addCircleBody(Vector2 pos, float radius, BodyType type, GameObject ref, float mass, boolean sensor, String filter)
	{
		float area = (float) (Math.PI*radius*radius);
		float density = mass/area;
		
		BodyDef bd = new BodyDef();
		
		bd.type = type;
		bd.fixedRotation = true;
		bd.position.set(pos);
		
		Body b = world.createBody(bd);
		
		CircleShape shape = new CircleShape();
		shape.setRadius(radius);
		
		Fixture f = b.createFixture(shape, density);
		f.setSensor(sensor);
		//f.setFilterData(collisionFilters.get(filter));
		
		b.setUserData(ref);
		b.resetMassData();
		
		shape.dispose();
		
		return b;
	}
}
