package com.electricsunstudio.xball.levels;

import com.badlogic.gdx.graphics.Color;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.BallNotify;
import com.electricsunstudio.xball.objects.GoalSensor;
import com.electricsunstudio.xball.objects.Ball;

/**
 *
 * @author toni
 */
public abstract class OneVOne extends Level {
	GoalSensor playerGoal,opponentGoal;
	int playerScore, opponentScore;
	
	@Override
	public void init()
	{
		playerGoal = Game.inst.gameObjectSystem.getObjectByName("player_goal", GoalSensor.class);
		opponentGoal = Game.inst.gameObjectSystem.getObjectByName("opponent_goal", GoalSensor.class);
		
		playerGoal.notifier = new PlayerGoalHit();
		opponentGoal.notifier = new OpponentGoalHit();
	}

	@Override
	public void render() {
		//show score
		
		Game.inst.drawTextCentered(Color.WHITE, String.valueOf(playerScore), Game.inst.screenWidth/2 - 100, Game.inst.screenHeight-100);
		Game.inst.drawTextCentered(Color.WHITE, String.valueOf(opponentScore), Game.inst.screenWidth/2 + 100, Game.inst.screenHeight-100);
	}

	//in either case, remove the ball
	private class GoalHit implements BallNotify {

		@Override
		public void onReceived(Ball ball)
		{
			ball.expire();
		}
	}

	private class PlayerGoalHit extends GoalHit {

		@Override
		public void onReceived(Ball ball)
		{
			++opponentScore;
			super.onReceived(ball);
		}
	}
	
	private class OpponentGoalHit extends GoalHit {

		@Override
		public void onReceived(Ball ball)
		{
			++playerScore;
			super.onReceived(ball);
		}
	}

}
