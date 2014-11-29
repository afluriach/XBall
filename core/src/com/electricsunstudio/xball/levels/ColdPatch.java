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
    
    public static final String name = "Cold Patch";
}
