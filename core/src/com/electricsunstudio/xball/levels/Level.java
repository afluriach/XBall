package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.BallNotify;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.objects.Ball;
import com.electricsunstudio.xball.objects.BlueBall;
import com.electricsunstudio.xball.objects.Bomb;
import com.electricsunstudio.xball.objects.GoalSensor;
import com.electricsunstudio.xball.objects.GoldBall;
import com.electricsunstudio.xball.objects.SpawnSensor;
import java.util.ArrayList;

/**
 *
 * @author toni
 */
public abstract class Level
{
    GoalSensor[] goalSensors;
    int[] scores;
    int players;
    
    SpawnSensor spawnSensor;
    int numBallsInPlay;
    
    SpawnSensor[] bombSensor;
    int numBombSensors;
    float nextBomb = 0;
    float minBombInterval;
    float maxBombInterval;
    
    public Level(int players, int ballsInPlay, int minBombInterval, int maxBombInterval)
    {
        this(players,ballsInPlay,minBombInterval,maxBombInterval,1);
    }
    
    public Level(int players, int ballsInPlay, int minBombInterval, int maxBombInterval, int numBombSensors)
    {
        this.players = players;
        numBallsInPlay = ballsInPlay;
        this.minBombInterval = minBombInterval;
        this.maxBombInterval = maxBombInterval;
        this.numBombSensors = numBombSensors;
        
        goalSensors = new GoalSensor[players];
        scores = new int[players];
    }
    
    public abstract String getMapName();
    public abstract String getPlayerName();
    
    public void init()
    {
        spawnSensor = Game.inst.gameObjectSystem.getObjectByName("spawn_sensor", SpawnSensor.class);
        
        for(int i=0;i<players; ++i)
        {
            goalSensors[i] = Game.inst.gameObjectSystem.getObjectByName("goal"+(i+1), GoalSensor.class);
            goalSensors[i].notifier = new PlayerGoalHit(i);
        }
        nextBomb = nextBombTime();
        
        bombSensor = new SpawnSensor[numBombSensors];
        
        for(int i=0;i<numBombSensors; ++i)
        {
            bombSensor[i] = Game.inst.gameObjectSystem.getObjectByName("bomb_sensor"+(i+1), SpawnSensor.class);
        }
    }
    
    float nextBombTime()
    {
        return minBombInterval + (maxBombInterval-minBombInterval)*Game.inst.rand.nextFloat();
    }
    
    public void update() {
        int count = Game.inst.gameObjectSystem.countObjectsByType(GoldBall.class) + 
                    Game.inst.gameObjectSystem.countObjectsByType(BlueBall.class);
        
        if(count < numBallsInPlay)
        {
            spawn();
        }
        
        nextBomb -= Game.SECONDS_PER_FRAME;
        
        if(nextBomb <= 0)
        {
            nextBomb = nextBombTime();
            spawnBomb();
        }
    }
    
    void spawn()
    {
        Vector2 pos = spawnSensor.findSpawnPos();
        Game.inst.gameObjectSystem.addObject(Game.inst.rand.nextBoolean() ? new GoldBall(pos) : new BlueBall(pos));
    }
    
    void spawnBomb()
    {
        ArrayList<Vector2> spawnPos = new ArrayList<Vector2>();
        
        for(SpawnSensor s : bombSensor)
        {
            spawnPos.addAll(s.findAllSpawnPos());
        }
        Vector2 pos = spawnPos.get(Game.inst.rand.nextInt(spawnPos.size()));
        Game.inst.gameObjectSystem.addObject(new Bomb(pos));
    }
    
    public void render()
    {
        //show score
        for(int i=0;i<scores.length; ++i)
        {
            Game.inst.drawTextCentered(Color.WHITE, String.valueOf(scores[i]), Game.inst.screenWidth/2 - scores.length*25 + i*50, Game.inst.screenHeight-100);
        }
    }
    
    public LevelState getState()
    {
        LevelState s = new LevelState();
        s.scores = scores;
        s.nextBombTime = nextBomb;
        
        return s;
    }
    
    public void restoreFromState(LevelState s)
    {
        scores = s.scores;
    }

    //in any case, remove the ball
    class GoalHit implements BallNotify {

        @Override
        public void onReceived(Ball ball)
        {
            ball.expire();
        }
    }

    //create a notifier parameterized by player ID, so the appropriate score is
    //updated
    class PlayerGoalHit extends GoalHit {
        int id;
        PlayerGoalHit(int playerId)
        {
            id = playerId;
        }
        @Override
        public void onReceived(Ball ball)
        {
            if(ball instanceof GoldBall)
                ++scores[id];
            else if(ball instanceof BlueBall)
                --scores[id];
            
            super.onReceived(ball);
        }
    }
}