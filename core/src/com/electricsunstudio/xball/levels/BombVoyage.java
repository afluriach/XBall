package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.objects.Ball;
import com.electricsunstudio.xball.objects.GoldBall;
import com.electricsunstudio.xball.objects.Bomb;
import com.electricsunstudio.xball.objects.SpawnSensor;
import com.electricsunstudio.xball.HitListener;
import com.electricsunstudio.xball.HitType;
import com.electricsunstudio.xball.objects.Player;

/**
 *
 * @author toni
 */
public class BombVoyage extends Level implements HitListener {
	int playerScore = 10;
	int opponentScore = 10;
	
	SpawnSensor bombSensor;
	float lastBomb = 0;
	float bombInterval = 2;
	
	String playerName = "blue_player";
	
	@Override
	public String getMapName()
	{
		return "bomb_voyage";
	}
	
	@Override
	public String getPlayerName()
	{
		return playerName;
	}
	
	@Override
	public void update() {
		lastBomb += Game.SECONDS_PER_FRAME;
		
		if(lastBomb >= bombInterval)
		{
			lastBomb -= bombInterval;
			spawnBomb();
		}
	}
	
	void spawnBomb()
	{
		Vector2 pos = bombSensor.findSpawnPos();
		Game.inst.gameObjectSystem.addObject(new Bomb(pos));
	}
	
	@Override
	public void init()
	{
		bombSensor = Game.inst.gameObjectSystem.getObjectByName("bomb_sensor", SpawnSensor.class);
	}

	@Override
	public void render() {
		//show score
		Game.inst.drawTextCentered(Color.WHITE, String.valueOf(playerScore), Game.inst.screenWidth/2 - 100, Game.inst.screenHeight-100);
		Game.inst.drawTextCentered(Color.WHITE, String.valueOf(opponentScore), Game.inst.screenWidth/2 + 100, Game.inst.screenHeight-100);
	}

	@Override
	public void onHit(Player player, HitType type) {
		if(player.getName().equals(playerName))
		{
			--playerScore;
		}
		else
		{
			--opponentScore;
		}
	}
}
