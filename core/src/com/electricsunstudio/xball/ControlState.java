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
    public Vector2 movePos = Vector2.Zero;
    public Vector2 aimPos = Vector2.Zero;
    public String player;
    public int frameNum;
}
