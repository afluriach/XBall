package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public class Level1 extends OneVOne {

    public Level1() {
        super(2,10,20);
    }
    
    @Override
    public String getMapName()
    {
        return "stadium1";
    }
    
    @Override
    public String getPlayerName()
    {
        return "blue_player";
    }
    
    public static final String name = "Stadium 1";
}
