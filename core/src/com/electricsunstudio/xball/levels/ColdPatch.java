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
        return mapName;
    }
    
    public static final String name = "Cold Patch";
    public static final String mapName = "t_cold_patch";
}
