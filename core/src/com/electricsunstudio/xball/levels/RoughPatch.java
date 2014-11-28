package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public class RoughPatch extends FourWay {

    public RoughPatch() {
        super(3, 7, 15, 3);
    }
    
    @Override
    public String getMapName()
    {
        return "t_rough_patch";
    }
    
    @Override
    public String getPlayerName()
    {
        return "player1";
    }
    
    public static final String name = "Rough Patch";
}
