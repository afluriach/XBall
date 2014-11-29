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
        return mapName;
    }
    
    public static final String name = "Bombs Away!";
    public static final String mapName = "bombs_away";
}
