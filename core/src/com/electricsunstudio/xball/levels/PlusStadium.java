package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.objects.Ball;
import com.electricsunstudio.xball.objects.GoldBall;
import com.electricsunstudio.xball.objects.BlueBall;

/**
 *
 * @author toni
 */
public class PlusStadium extends FourWay {
    
    @Override
    public String getMapName()
    {
        return "t";
    }
    
    @Override
    public String getPlayerName()
    {
        return "player1";
    }
    
    public static final String name = "+ Stadium";
    
    @Override
    public void update() {
        int count = Game.inst.gameObjectSystem.countObjectsByType(Ball.class);
        
        if(count < 2)
        {
            spawn();
        }
    }
    
    void spawn()
    {
        Vector2 pos = spawnSensor.findSpawnPos();
        Game.inst.gameObjectSystem.addObject(Game.inst.rand.nextBoolean() ? new GoldBall(pos) : new BlueBall(pos));
    }
}
