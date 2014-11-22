package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.objects.Ball;
import com.electricsunstudio.xball.objects.GoldBall;

/**
 *
 * @author toni
 */
public class Level1 extends OneVOne {
    
    @Override
    public String getMapName()
    {
        return "stadium1";
    }
    
    @Override
    public String getPlayerName()
    {
        return "blue_player";
    }
    
    public static final String name = "Stadium 1";
    
    @Override
    public void update() {
        int count = Game.inst.gameObjectSystem.countObjectsByType(Ball.class);
        
        if(count == 0)
        {
            spawn();
        }
    }
    
    void spawn()
    {
        Vector2 pos = spawnSensor.findSpawnPos();
        Game.inst.gameObjectSystem.addObject(new GoldBall(pos));
    }
}
