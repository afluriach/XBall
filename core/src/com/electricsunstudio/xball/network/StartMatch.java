package com.electricsunstudio.xball.network;

import java.io.Serializable;

/**
 *
 * @author toni
 */
public class StartMatch extends ServerIntent implements Serializable {
    private static final long serialVersionUID = -5956017416108785249L;
    public String levelName;
    public String player;
    public long seed;

    public StartMatch(String levelName, String player, long seed) {
        this.levelName = levelName;
        this.player = player;
        this.seed = seed;
    }
}
