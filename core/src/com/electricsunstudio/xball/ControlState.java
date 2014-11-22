package com.electricsunstudio.xball;

import java.io.Serializable;

/**
 *
 * @author toni
 */
public class ControlState implements Serializable{
    private static final long serialVersionUID = 6677073694301387998L;
    public boolean grab = false;
    public boolean kick = false;
    public boolean special = false;
    public boolean lock = false;
    public float moveX;
    public float moveY;
    public String player;
    public int frameNum;
}
