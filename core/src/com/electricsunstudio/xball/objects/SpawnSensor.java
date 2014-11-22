package com.electricsunstudio.xball.objects;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.GameObject;
import java.util.ArrayList;

/**
 *
 * @author toni
 */
public class SpawnSensor extends GameObject
{
    Rectangle rect;
    
    public SpawnSensor(MapObject mo)
    {
        super(mo);
        
        rect = Game.toTilespace(((RectangleMapObject)mo).getRectangle());
    }
    @Override
    public void update()
    {
    }
    
    @Override
    public void handleContact(GameObject other)
    {
    }
    @Override
    public void handleEndContact(GameObject other)
    {
        
    }
    @Override
    public void init()
    {
        
    }
    
    @Override
    public Vector2 getCenterPos()
    {
        return rect.getCenter(new Vector2());
    }
    
    //choose a random tile that is not occupied
    public Vector2 findSpawnPos()
    {
        ArrayList<Vector2> points = new ArrayList();
        
        Rectangle tmp = new Rectangle();
        tmp.width = tmp.height = 1;
        
        for(float y = rect.y; y < rect.y + rect.height; ++y)
        {
            tmp.y = y;
            for(float x = rect.x; x < rect.x + rect.width; ++x)
            {
                tmp.x = x;
                if(!Game.inst.physics.checkSpace(tmp))
                {
                    points.add(new Vector2(x+0.5f,y+0.5f));
                }
            }
        }
        
        return points.get(Game.inst.rand.nextInt(points.size()));
    }
}
