package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.objects.GoldBall;
import com.electricsunstudio.xball.objects.Bomb;
import com.electricsunstudio.xball.objects.SpawnSensor;

/**
 *
 * @author toni
 */
public class CoolBlast extends OneVOne {
    SpawnSensor bombSensor;
    float lastBomb = 0;
    float bombInterval = 2;
    
    @Override
    public String getMapName()
    {
        return "cool_blast";
    }
    
    public static final String name =  "Cool Blast";
    
    @Override
    public String getPlayerName()
    {
        return "blue_player";
    }
    
    @Override
    public LevelState getState()
    {
        LevelState s = new LevelState();
        s.scores = new int[2];
        s.scores[0] = playerScore;
        s.scores[1] = opponentScore;
        s.lastBombTime = lastBomb;
        return s;
    }
    
    @Override
    public void restoreFromState(LevelState s)
    {
        lastBomb = s.lastBombTime;
        playerScore = s.scores[0];
        opponentScore = s.scores[1];
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
