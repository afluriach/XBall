package com.electricsunstudio.xball;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import java.util.ArrayList;
import java.util.Map;
import java.util.EnumMap;


/**
 *
 * @author toni
 */
public class Physics {
	public static final int VELOCITY_ITERATIONS = 8;
	public static final int POSITION_ITERATIONS = 3;
	
	World world;
	
	public static final short
		playerCategory = 1,
		ballCategory = 2,
		wallCategory = 4,
		ballSensorCategory = 8;

	//the mask type is short but the result of the expression is int
	public static void putFilter(short category, int mask, FilterClass cls, Map map)
	{
		Filter filter = new Filter();
		filter.categoryBits = category;
		filter.maskBits = (short) mask;
		map.put(cls, filter);
	}
	
	public static Map<FilterClass, Filter> collisionFilters = new EnumMap<FilterClass,Filter>(FilterClass.class) {{
		
		putFilter(playerCategory,
			playerCategory | ballCategory | wallCategory,
			FilterClass.player,
			this);
		
		putFilter(ballCategory,
			playerCategory | ballCategory | wallCategory | ballSensorCategory,
			FilterClass.ball,
			this);
		
		putFilter(wallCategory,
			playerCategory | ballCategory ,
			FilterClass.wall,
			this);
		
		putFilter(ballSensorCategory,
				  ballCategory,
				  FilterClass.ballSensor,
				  this);
	}};

	public Physics()
	{
		//no gravity, allow sleeping objects
		world = new World(new Vector2(0,0), true);
		world.setContactListener(new ContactHandler());
	}
	
	public Body addCircleBody(Vector2 pos, float radius, BodyType type, GameObject ref, float mass, boolean sensor, FilterClass filter)
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
		f.setFilterData(collisionFilters.get(filter));
		
		b.setUserData(ref);
		b.resetMassData();
		
		shape.dispose();
		
		return b;
	}
	
	public Body addRectBody(MapObject mo,
							BodyType type,
							GameObject ref,
							float mass,
							boolean sensor,
							FilterClass filter)
	{
		Rectangle pixRect = ((RectangleMapObject)mo).getRectangle();
		
		return addRectBody(Game.mapObjectPos(mo),
						   pixRect.height*Game.TILES_PER_PIXEL,
						   pixRect.width*Game.TILES_PER_PIXEL,
						   type,
						   ref,
						   mass,
						   sensor,
						   filter);
	}

	
	public Body addRectBody(Vector2 pos,
						float height,
						float width,
						BodyType type,
						GameObject ref,
						float mass,
						boolean sensor,
						FilterClass filter)
	{
		float area = height*width;
		float density = mass/area;
		
		BodyDef bd = new BodyDef();
		
		bd.type = type;
		bd.fixedRotation = true;
		bd.position.set(pos);
		
		Body b = world.createBody(bd);
		
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(width/2, height/2);
		
		Fixture f = b.createFixture(shape, density);
		f.setSensor(sensor);
		
		b.setUserData(ref);
		b.resetMassData();

		f.setFilterData(collisionFilters.get(filter));
		
		shape.dispose();
		
		return b;
	}
	
	public void update()
	{
		Game.inst.gameObjectSystem.applyAccel();
		world.step(Game.SECONDS_PER_FRAME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
	}
	
	void queryAABB(Rectangle rect, QueryCallback cb)
	{
		world.QueryAABB(cb, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);        
	}

	/**
	 * check to see if there is any object obstructing a given space
	 * @param rect the area to check
	 * @return whether or not there is an object present
	 */
	public boolean checkSpace(Rectangle rect)
	{
		DetectObjectCallback cb = new DetectObjectCallback();
		queryAABB(rect, cb);
		return cb.detected();
	}
	
	public ArrayList<GameObject> getWithinSpace(Rectangle rect)
	{
		ObjectsInSpaceCallback cb = new ObjectsInSpaceCallback();
		queryAABB(rect, cb);
		return cb.detected;
	}
	
	public void removeBody(Body b)
	{
		world.destroyBody(b);
	}

	public static void setRestitution(Body b, float restitution)
	{
		b.getFixtureList().get(0).setRestitution(restitution);
	}
}
