package com.electricsunstudio.xball.network;

import java.io.Serializable;

/**
 *
 * @author toni
 */
public class LoginResponse extends ServerIntent implements Serializable{
    private static final long serialVersionUID = -7155814589526681538L;
    public String msg;
    public boolean success;

    public LoginResponse(String msg, boolean success) {
        this.msg = msg;
        this.success = success;
    }
    
}
