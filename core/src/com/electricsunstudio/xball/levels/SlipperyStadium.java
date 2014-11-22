package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.objects.Ball;
import com.electricsunstudio.xball.objects.GoldBall;

/**
 *
 * @author toni
 */
public class SlipperyStadium extends OneVOne {
    
    @Override
    public String getMapName()
    {
        return "slippery_stadium";
    }
    
    @Override
    public String getPlayerName()
    {
        return "blue_player";
    }
    
    public static final String name =  "Slippery Stadium";
    
    @Override
    public void update() {
        int count = Game.inst.gameObjectSystem.countObjectsByType(Ball.class);
        
        if(count < 3)
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
