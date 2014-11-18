package com.electricsunstudio.xball.physics;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.GameObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.EnumMap;
import java.util.List;


/**
 *
 * @author toni
 */
public class Physics {
	public static final int VELOCITY_ITERATIONS = 8;
	public static final int POSITION_ITERATIONS = 3;
	
	public static final float GRAVITY = 9.8f;
	
	World world;
	Box2DDebugRenderer debugRenderer;
	
	public static final short
		playerCategory = 1,
		ballCategory = 2,
		wallCategory = 4,
		ballSensorCategory = 8;

	//the mask type is short but the result of the expression is int
	public static void putFilter(short category, int mask, FilterClass cls)
	{
		Filter filter = new Filter();
		filter.categoryBits = category;
		filter.maskBits = (short) mask;
		collisionFilters.put(cls, filter);
	}
	
	public static Map<FilterClass, Filter> collisionFilters;

	public Physics()
	{
		//no gravity, allow sleeping objects
		world = new World(new Vector2(0,0), true);
		world.setContactListener(new ContactHandler());
		
		debugRenderer = new Box2DDebugRenderer();
		debugRenderer.setDrawBodies(true);
		debugRenderer.setDrawContacts(true);
		debugRenderer.setDrawJoints(true);
		debugRenderer.setDrawInactiveBodies(true);
		
		collisionFilters = new EnumMap<FilterClass,Filter>(FilterClass.class);
		
		putFilter(playerCategory,
			playerCategory | ballCategory | wallCategory,
			FilterClass.player);
		
		putFilter(ballCategory,
			playerCategory | ballCategory | wallCategory | ballSensorCategory,
			FilterClass.ball);
		
		putFilter(wallCategory,
			playerCategory | ballCategory ,
			FilterClass.wall);
		
		putFilter(ballSensorCategory,
				  ballCategory,
				  FilterClass.ballSensor);
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
		b.setFixedRotation(false);
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
	
	//remove joints when body is removed
	public void removeBody(Body b)
	{
		while(b.getJointList().size > 0)
		{
			JointEdge e = b.getJointList().get(0);
			e.joint.setUserData(Boolean.TRUE);
			world.destroyJoint(e.joint);
		}
		world.destroyBody(b);
	}

	public static void setRestitution(Body b, float restitution)
	{
		b.getFixtureList().get(0).setRestitution(restitution);
	}
	
	public List<Joint> joinBodies(Body a, Body b)
	{
		ArrayList<Joint> joints = new ArrayList();
		final float ratio = 10;
		final float freq = Game.FRAMES_PER_SECOND/2;
		final float disp = 0.3f;
		
		DistanceJointDef defA = new DistanceJointDef();
		defA.initialize(a, b, a.getPosition().add(-disp,0), b.getPosition().add(disp, 0));
		defA.dampingRatio = ratio;
		defA.frequencyHz = freq;
		
		DistanceJointDef defB = new DistanceJointDef();
		defB.initialize(a, b, a.getPosition().add(disp,0), b.getPosition().add(-disp, 0));
		defB.dampingRatio = ratio;
		defB.frequencyHz = freq;

		DistanceJointDef defC = new DistanceJointDef();
		defC.initialize(a, b, a.getPosition().add(0,disp), b.getPosition().add(0,-disp));
		defC.dampingRatio = ratio;
		defC.frequencyHz = freq;

		DistanceJointDef defD = new DistanceJointDef();
		defD.initialize(a, b, a.getPosition().add(0,-disp), b.getPosition().add(0, disp));
		defD.dampingRatio = ratio;
		defD.frequencyHz = freq;

		joints.add(world.createJoint(defA));
		joints.add(world.createJoint(defB));
		joints.add(world.createJoint(defC));
		joints.add(world.createJoint(defD));
		
		return joints;
	}
	
	//to avoid double removing, check if either attached body has already expired
	public void removeJoint(Joint j)
	{
		if(j.getUserData() != Boolean.TRUE)
		{
			Game.log("destroy joint ");
			world.destroyJoint(j);
			j.setUserData(Boolean.TRUE);
		}
	}
	
	public void debugRender(Matrix4 transform)
	{
		debugRenderer.render(world, transform.cpy().scale(Game.PIXELS_PER_TILE, Game.PIXELS_PER_TILE, 1));
	}
}
