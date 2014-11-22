package com.electricsunstudio.xball.network;

/**
 *
 * @author toni
 */
public class StartMatch extends ServerIntent {
    public String levelName;
    public String player;

    public StartMatch(String levelName, String player) {
        this.levelName = levelName;
        this.player = player;
    }
}
