package com.electricsunstudio.xball.levels;

import java.io.Serializable;

/**
 *
 * @author toni
 */
public class LevelState implements Serializable {
    private static final long serialVersionUID = -6908891545470788582L;
    
    int playerScore;
    int opponentScore;
    float lastBombTime;

    public LevelState(int playerScore, int opponentScore) {
        this.playerScore = playerScore;
        this.opponentScore = opponentScore;
    }
    
}
