package com.electricsunstudio.xball;

import com.badlogic.gdx.math.Vector2;
import java.util.EnumMap;

/**
 *
 * @author toni
 */
public class ControlState {
    public boolean grab = false;
    public boolean kick = false;
    public boolean special = false;
    public boolean lock = false;
    public float moveX;
    public float moveY;
    public String player;
    public int frameNum;
}
