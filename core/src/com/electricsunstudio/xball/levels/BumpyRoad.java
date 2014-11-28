package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public class BumpyRoad extends OneVOne {

    public BumpyRoad() {
        super(2, 10, 20);
    }
    
    @Override
    public String getMapName()
    {
        return "bumpy_road";
    }
    
    @Override
    public String getPlayerName()
    {
        return "blue_player";
    }
    
    public static final String name = "Bumpy Road";
}
