package com.electricsunstudio.xball.network;

/**
 *
 * @author toni
 */
public class LoginResponse extends ServerIntent{
    public String msg;
    public boolean success;

    public LoginResponse(String msg, boolean success) {
        this.msg = msg;
        this.success = success;
    }
    
}
