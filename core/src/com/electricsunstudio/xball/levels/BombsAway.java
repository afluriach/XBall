package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.objects.Ball;
import com.electricsunstudio.xball.objects.GoldBall;
import com.electricsunstudio.xball.objects.Bomb;
import com.electricsunstudio.xball.objects.SpawnSensor;

/**
 *
 * @author toni
 */
public class BombsAway extends OneVOne {
    SpawnSensor bombSensor;
    float lastBomb = 0;
    float bombInterval = 2;
    
    @Override
    public String getMapName()
    {
        return "bombs_away";
    }
    
    public static final String name = "Bombs Away!";
    
    @Override
    public String getPlayerName()
    {
        return "blue_player";
    }
    
    @Override
    public void update() {
        int count = Game.inst.gameObjectSystem.countObjectsByType(GoldBall.class);
        
        if(count == 0)
        {
            spawn();
        }
        
        lastBomb += Game.SECONDS_PER_FRAME;
        
        if(lastBomb >= bombInterval)
        {
            lastBomb -= bombInterval;
            spawnBomb();
        }
    }
    
    void spawn()
    {
        Vector2 pos = spawnSensor.findSpawnPos();
        Game.inst.gameObjectSystem.addObject(new GoldBall(pos));
    }
    
    void spawnBomb()
    {
        Vector2 pos = bombSensor.findSpawnPos();
        Game.inst.gameObjectSystem.addObject(new Bomb(pos));
    }
    
    @Override
    public void init()
    {
        super.init();
        
        bombSensor = Game.inst.gameObjectSystem.getObjectByName("bomb_sensor", SpawnSensor.class);
    }
}
