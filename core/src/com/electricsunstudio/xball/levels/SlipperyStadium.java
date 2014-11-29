package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public class SlipperyStadium extends OneVOne {

    public SlipperyStadium() {
        super(2, 20, 30);
    }
    
    
    @Override
    public String getMapName()
    {
        return mapName;
    }
    
    public static final String mapName = "slippery_stadium";
    public static final String name =  "Slippery Stadium";
}
