package com.electricsunstudio.xball.levels;

/**
 *
 * @author toni
 */
public abstract class Level
{
	public abstract String getMapName();
	public abstract String getPlayerName();
	
	public abstract void init();
	public abstract void update();
	public abstract void render();
}