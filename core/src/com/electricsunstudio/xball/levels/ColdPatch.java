package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public class ColdPatch extends FourWay {

    public ColdPatch() {
        super(3, 10, 20, 3);
    }
    
    @Override
    public String getMapName()
    {
        return "t_cold_patch";
    }
    
    @Override
    public String getPlayerName()
    {
        return "player1";
    }
    
    public static final String name = "Cold Patch";
}
