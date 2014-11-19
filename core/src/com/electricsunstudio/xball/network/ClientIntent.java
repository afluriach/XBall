package com.electricsunstudio.xball.network;

import java.io.Serializable;

public class ClientIntent implements Serializable
{
	public ClientAction action;
	public String map;
	public String username;

	public ClientIntent() {
	}

	public ClientIntent(ClientAction action, String map, String username) {
		this.action = action;
		this.map = map;
		this.username = username;
	}
}
