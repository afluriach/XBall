package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.graphics.Color;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.BallNotify;
import com.electricsunstudio.xball.objects.GoalSensor;
import com.electricsunstudio.xball.objects.Ball;
import com.electricsunstudio.xball.objects.GoldBall;
import com.electricsunstudio.xball.objects.BlueBall;
import com.electricsunstudio.xball.objects.SpawnSensor;

/**
 *
 * @author toni
 */
public abstract class FourWay extends Level {
    GoalSensor[] goalSensors = new GoalSensor[4];
    int[] scores = new int[4];
    SpawnSensor spawnSensor;
    
    @Override
    public LevelState getState()
    {
        LevelState s = new LevelState();
        s.scores = scores;
        
        return s;
    }
    
    @Override
    public void restoreFromState(LevelState s)
    {
        scores = s.scores;
    }
    
    @Override
    public void init()
    {
        for(int i=0;i<4; ++i)
        {
            goalSensors[i] = Game.inst.gameObjectSystem.getObjectByName("goal"+(i+1), GoalSensor.class);
            goalSensors[i].notifier = new PlayerGoalHit(i);
        }
        spawnSensor = Game.inst.gameObjectSystem.getObjectByName("spawn_sensor", SpawnSensor.class);        
    }

    @Override
    public void render() {
        //show score
        for(int i=0;i<4; ++i)
        {
            Game.inst.drawTextCentered(Color.WHITE, String.valueOf(scores[i]), Game.inst.screenWidth/2 - 100 + i*50, Game.inst.screenHeight-100);
        }        
    }

    //in any case, remove the ball
    private class GoalHit implements BallNotify {

        @Override
        public void onReceived(Ball ball)
        {
            ball.expire();
        }
    }

    //create a notifier parameterized by player ID, so the appropriate score is
    //updated
    private class PlayerGoalHit extends GoalHit {
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
