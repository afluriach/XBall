package com.electricsunstudio.xball;

import com.electricsunstudio.xball.levels.LevelState;
import java.io.Serializable;

/**
 *
 * @author toni
 */
public class GameState implements Serializable
{
    private static final long serialVersionUID = -6847059309100848491L;
    int frameNum;
    GameObjectSystemState objectState;
    LevelState levelState;

    public GameState(int frameNum, GameObjectSystemState objectState, LevelState levelState) {
        this.frameNum = frameNum;
        this.objectState = objectState;
        this.levelState = levelState;
    }
    
    
}
