package com.electricsunstudio.xball.network;

/**
 *
 * @author toni
 */
public class ConnectIntent extends ClientIntent{
	public String username;

	public ConnectIntent(String username) {
		this.username = username;
	}
	
}
