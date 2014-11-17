package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.objects.Ball;
import com.electricsunstudio.xball.objects.GoldBall;

/**
 *
 * @author toni
 */
public class BumpyRoad extends OneVOne {
	
	@Override
	public String getMapName()
	{
		return "bumpy_road";
	}
	
	public String getPlayerName()
	{
		return "blue_player";
	}
	
	@Override
	public void update() {
		int count = Game.inst.gameObjectSystem.countObjectsByType(Ball.class);
		
		if(count < 3)
		{
			spawn();
		}
	}
	
	void spawn()
	{
		Vector2 pos = spawnSensor.findSpawnPos();
		Game.inst.gameObjectSystem.addObject(new GoldBall(pos));
	}
}
