package com.electricsunstudio.xball.network;

import java.io.Serializable;

/**
 *
 * @author toni
 */
public class ConnectIntent extends ClientIntent implements Serializable{
    private static final long serialVersionUID = -2260944382774199403L;
    public String username;

    public ConnectIntent(String username) {
        this.username = username;
    }
    
}
