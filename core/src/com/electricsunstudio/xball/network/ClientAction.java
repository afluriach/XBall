package com.electricsunstudio.xball.network;

import java.io.Serializable;

public enum ClientAction implements Serializable
{	
	connect,
	disconnect,
	createMatch,
	destroyMatch,
	joinMatch,
	listMatches;
	
	public static final long serialVersionUID = 9574947777L;
}
