package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public class PlusStadium extends FourWay {

    public PlusStadium() {
        super(3, 7, 15, 3);
    }
    
    @Override
    public String getMapName()
    {
        return "t";
    }
    
    public static final String name = "+ Stadium";
}
