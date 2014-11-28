package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public class BombsAway extends OneVOne {

    public BombsAway() {
        super(2, 5,10);
    }
    
    @Override
    public String getMapName()
    {
        return "bombs_away";
    }
    
    public static final String name = "Bombs Away!";
    
    @Override
    public String getPlayerName()
    {
        return "blue_player";
    }    
}
