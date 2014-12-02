package com.electricsunstudio.xball;

import com.electricsunstudio.xball.physics.Physics;
import com.badlogic.gdx.graphics.g2d.Sprite;
import java.util.NoSuchElementException;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.*;
import java.lang.reflect.Constructor;

public abstract class GameObject 
{
    //for an object type loaded from a Tiled map, prepend this package prefix
    //to get the full path name. i.e. all objects that can be loaded by name
    //will be in this package (including subpackages)
    public static final String basePackage = "com.electricsunstudio.xball.objects";
    public float radius;

    public static Class<?> getObjectClass(String name)
    {
        if(name == null) throw new NullPointerException("null name given");
        if(name.equals("")) throw new IllegalArgumentException("blank name given");

        //check if a valid class exists with a given name
        //every class loaded by name has to be in the com.ESS.xball.objects package.
        
        //concat object package.
        //the type used in the map could still have a package path
        String classPath = basePackage + "." + name;

        Class cls = null;
        try {
            cls = Class.forName(classPath);
        } catch (ClassNotFoundException ex) {
            throw new NoSuchElementException("object class " + name + " not found");
        }

        if(!GameObject.class.isAssignableFrom(cls))
        {
            //the class is not a subclass of GameObject
            throw new IllegalArgumentException("class " + classPath + " is not derived from GameObject");
        }

        return cls;
    }
    
    public static GameObject instantiate(MapObject mo)
    {
        String type = mo.getProperties().get("type", String.class);
        
        if(type == null || type.equals(""))
        {
            throw new RuntimeException(String.format("Object %s has no type.", mo.getName()));
        }
        
        Class<?> cls = GameObject.getObjectClass(type);
        Constructor<?> cons = null;
        
        try
        {
            cons = cls.getConstructor(MapObject.class);
        } catch (Exception ex) {
            Game.log(String.format("Object %s, class %s has no suitable constructor.", mo.getName(), type));
        }
        
        try{
            return (GameObject) cons.newInstance(mo);
        }
        catch(Exception ex){
            Game.log( String.format("Error in object constructor, class: %s, name: %s", type, mo.getName()));
            ex.printStackTrace();
            throw new RuntimeException("constructor exception");
        }
    }
    
    static int nextUid = 0;
    
    //physics
    public Body physicsBody;
    public static final float iceTraction = 0.3f;
    public static final float roughResistance = 0.5f;
    
    String name;
    boolean expired = false;
    int uid;

    public Sprite sprite;
    
    Vector2 crntAcceleration;
    
    public GameObject(MapObject mo)
    {
        name = mo.getName();
        uid = nextUid++;
    }
    
    public GameObject(String name)
    {
        this.name = name;
        uid = nextUid++;
    }
    
    public int getUid()
    {
        return uid;
    }
    
    public void setRotation(Vector2 v)
    {
        //vector2 and sprite angle conventions are different
        float rotation = v.angle();
        
        physicsBody.setTransform(getCenterPos(), (float)Math.toRadians(rotation));
    }
    
    public void setRotationRad(float rad)
    {
        physicsBody.setTransform(getCenterPos(), rad);
    }
    
    public float getRotation()
    {
        return (float) Math.toDegrees(physicsBody.getAngle());
    }

    public float getRotationRad()
    {
        return physicsBody.getAngle();
    }

    
    public void expire()
    {
        expired = true;
    }
    
    public boolean isExpired()
    {
        return expired;
    }
        
    public static boolean allExpired(Iterable<GameObject> list)
    {
        for(GameObject go : list)
        {
            if(!go.isExpired())
                return false;
        }
        return true;
    }
    
    public Vector2 getCenterPos()
    {
        return physicsBody.getPosition().cpy();
    }
    
    public void setVel(Vector2 vel)
    {
        physicsBody.setLinearVelocity(vel);
    }
    
    public Vector2 getVel()
    {
        return physicsBody.getLinearVelocity().cpy();
    }   
    
    public void setPos(Vector2 pos)
    {
        physicsBody.setTransform(pos, physicsBody.getAngle());
    }
    
    public void setAccel(Vector2 acc)
    {
        crntAcceleration = acc;
    }
    
    public void applyAccel()
    {
        if(crntAcceleration != null)
        {
            Vector2 dv = crntAcceleration.cpy().scl(Game.SECONDS_PER_FRAME);
            setVel(getVel().add(dv));
        }
    }
    @Override
    public String toString()
    {
        return String.format("gameobject class: %s, name: %s", this.getClass().getSimpleName(), name);
    }
    
    public void onExpire()
    {
        if(physicsBody != null)
        {
            Game.inst.physics.removeBody(physicsBody);
        }
    }

    /**
     * 
     * @return returns the axis aligned bounding box based on the physics body.
     */
    public Rectangle getAABB()
    {
        //start with a rectangle of size 0,0 centered at the center of this body.
        //Since fixture coordinates are relative to center, start with the center of
        //the rectangle at the origin. 
        Vector2 center = Vector2.Zero;
        Rectangle box = new Rectangle();
        box.setCenter(center);
        box.height = 0;
        box.width = 0;
        
        for(Fixture f : physicsBody.getFixtureList())
        {
            if(f.getShape().getType() == Shape.Type.Polygon)
            {
                //consider each vertex in the polygon. if a point is not in the AABB, 
                //expand it to include that point.
                PolygonShape s = (PolygonShape) f.getShape();
                Vector2 vertex = new Vector2();
                
                for(int i=0;i<s.getVertexCount(); ++i)
                {
                    //vertex coordinates are relative to the center of the body. Or center of fixture(?)
                    //in this case, only one fixture centered on the body. 
                    s.getVertex(i, vertex);
                    
                    if(vertex.x < box.x)
                    {
                        box.width += box.x - vertex.x;
                        box.x = vertex.x;
                    }
                    else if(vertex.x > box.x + box.width)
                    {
                        box.width = vertex.x - box.x;
                    }
                    
                    if(vertex.y < box.y)
                    {
                        box.height += box.y - vertex.y;
                        box.y = vertex.y;
                    }
                    else if(vertex.y > box.y + box.height)
                    {
                        box.height = vertex.y - box.y;
                    }
                }
            }
            else if(f.getShape().getType() == Shape.Type.Circle)
            {
                CircleShape s = (CircleShape) f.getShape();
                
                //consider bounding square defining the circle.
                //check each of the four edges based on the four axis-aligned
                //points on the circle
                
                if(center.x - s.getRadius() < box.x)
                {
                    box.width += box.x - (center.x - s.getRadius());
                    box.x = center.x - s.getRadius();
                }
                if(center.x + s.getRadius() > box.x + box.width)
                {
                    box.width = center.x + s.getRadius() - box.x;
                }
                
                if(center.y - s.getRadius() < box.y)
                {
                    box.height += box.y - (center.y - s.getRadius());
                    box.y = center.y - s.getRadius();
                }
                if(center.y + s.getRadius() > box.y + box.height)
                {
                    box.height = center.y + s.getRadius() - box.y;
                }
                
            }
            else
            {
                throw new IllegalArgumentException("unsupported fixture shape: " + f.getShape().getType());
            }
                
        }
        
        //translate AABB based on the center of the GO      
        box.setCenter(getCenterPos());
        return box;
    }   
    
    public String getName()
    {
        return name;
    }   

    /**
     * set each of the GameObjects fixtures to 
     * @param sensor the value to set. if true, fixtures will still register collisions but will not have any solid physical presence.
     */
    public void setSensor(boolean sensor)
    {
        for(Fixture f : physicsBody.getFixtureList())
        {
            f.setSensor(sensor);
        }
    }
    
    public abstract void update();
    
    public Sprite getSprite()
    {
    	if(sprite == null) return null;

    	Game.setSpritePos(sprite, getCenterPos(), getRotation()-90);
    	return new Sprite(sprite);
    }
    
    public abstract void handleContact(GameObject other);
    public abstract void handleEndContact(GameObject other);
    public abstract void init();
    
    public void applyKineticFriction(float uk)
    {
        if(Game.inst.onMapLayer("ice", getCenterPos()))
            uk *= iceTraction;
        else if(Game.inst.onMapLayer("rough", getCenterPos()))
            uk += roughResistance;
        
        //impulse is F*t so
        //F*t =
        //m*a*t =
        //m*v/t*t
        //m*v
        //impulse / mass is dv
        float impulseMag = physicsBody.getMass()*Physics.GRAVITY*uk*Game.SECONDS_PER_FRAME;
        float dv = Physics.GRAVITY*uk*Game.SECONDS_PER_FRAME;
        
        if(getVel().len2() <= dv*dv)
        {
            //if dv >= velocity, kinetic friction will halt object in the next frame
            //set velocity to zero to avoid overshooting
            setVel(Vector2.Zero);
        }
        else
        {
            Vector2 impulse = getVel().scl(-impulseMag);
            physicsBody.applyLinearImpulse(impulse, getCenterPos(), true);
        }
    }
    
    public float getRadius()
    {
        return radius;
    }
    
    public GameObjectState getState()
    {
        return new GameObjectState(this);
    }
    
    public void restoreFromState(GameObjectState s)
    {
        this.name = s.name;
        this.uid = s.uid;
        s.applyState(this);
    }
}
