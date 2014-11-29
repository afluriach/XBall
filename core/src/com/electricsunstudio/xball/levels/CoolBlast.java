package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public class CoolBlast extends OneVOne {

    public CoolBlast() {
        super(2, 5, 10);
    }
    
    
    @Override
    public String getMapName()
    {
        return mapName;
    }
    
    public static final String name =  "Cool Blast";
    public static final String mapName =  "cool_blast";
}
