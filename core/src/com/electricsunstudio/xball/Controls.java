package com.electricsunstudio.xball;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

public class Controls {
	public static final int maxPressEvents = 5;
	
	public static final int margin = 20;
	public static final int buttonRadius = 40;
	public static final int buttonTrimThickness = 5;
	
	public static final int controlpadDiameter = 240;
	public static final float controlpadDeadzone = 1.0f/6;
	public static final float controlpadMax = 0.8f;
	public static final float actualRange = controlpadMax - controlpadDeadzone;

	//screen
	int width;
	int height;
	
	public Vector2 controlPadPos = Vector2.Zero;

	boolean kick, grab, lock = false;
	
	Circle controlPad, kickButton, grabButton, lockButton;
	
	Pair<Color,Color> kickColor = new Pair(Game.hsva(10f,.8f,.3f, 1f), Game.hsva(10f, 1f, .8f, 1f));
	Pair<Color,Color> grabColor = new Pair(Game.hsva(251f,.8f,.3f, 1f), Game.hsva(251f, 1f, .8f, 1f));
	Pair<Color,Color> lockColor = new Pair(Game.hsva(130f,.8f,.3f, 1f), Game.hsva(130f, 1f, .8f, 1f));
	
	Color controlPadOuterColor = Game.hsva(251, .3f, .7f, 1f);
	Color controlPadColor = Game.hsva(251f,.1f,.3f, 1f);
	Color controlPadInnerColor = Game.hsva(251f, .1f, .8f, 1f);
		
	public Controls()
	{
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();

		createShapes();
	}
	
	public void createShapes()
	{
		Vector2 controlpadCenter = new Vector2(margin+controlpadDiameter/2, margin+controlpadDiameter/2);

		controlPad = new Circle(controlpadCenter.x, controlpadCenter.y, controlpadDiameter/2);
		
		kickButton = new Circle(width-margin-buttonRadius, margin+buttonRadius, buttonRadius);
		grabButton = new Circle(width-margin-buttonRadius, margin + 4*buttonRadius, buttonRadius);
		lockButton = new Circle(width-margin-buttonRadius, margin+7*buttonRadius, buttonRadius);
	}

	public void drawButtonInner(ShapeRenderer shapeRenderer, boolean pressed, Pair<Color,Color> color, Circle button)
	{
		shapeRenderer.setColor(pressed ? color.b : color.a);
		shapeRenderer.circle(button.x, button.y, button.radius);
	}
	
	public void drawButtonOuter(ShapeRenderer shapeRenderer, Color color, Circle button)
	{
		shapeRenderer.setColor(color);
		shapeRenderer.circle(button.x, button.y, button.radius + buttonTrimThickness);
	}
	
	public void drawButton(ShapeRenderer shapeRenderer, boolean pressed, Pair<Color,Color> color, Circle button)
	{
		drawButtonOuter(shapeRenderer, color.b, button);
		drawButtonInner(shapeRenderer, pressed, color, button);
	}

	public void render(ShapeRenderer shapeRenderer)
	{
		shapeRenderer.begin(ShapeType.Filled);
		
		drawButton(shapeRenderer, kick, kickColor, kickButton);
		drawButton(shapeRenderer, grab, grabColor, grabButton);
		drawButton(shapeRenderer, lock, lockColor, lockButton);
		
		//draw outer margin
		shapeRenderer.setColor(controlPadOuterColor);
		shapeRenderer.circle(controlPad.x, controlPad.y, controlPad.radius);
		
		shapeRenderer.setColor(controlPadColor);
		shapeRenderer.circle(controlPad.x, controlPad.y, controlPad.radius*controlpadMax);

		shapeRenderer.setColor(controlPadInnerColor);
		shapeRenderer.circle(controlPad.x, controlPad.y, controlPad.radius*controlpadDeadzone);

		shapeRenderer.end();
	}
	
	private void handleTouchControls()
	{
		for(int i=0; i< maxPressEvents; ++i)
		{
			if(Gdx.input.isTouched(i))
				checkTouchPress(Gdx.input.getX(i), height - Gdx.input.getY(i));
			else break;
		}
	}
	
	//TODO if there are multiple presses on pad, use the one that is closest to the control pad
	//position in the previous frame
	private void checkTouchPress(int x, int y)
	{		
		Vector2 point = new Vector2(x,y);

		if(kickButton.contains(x,y)) kick = true;
		else if(grabButton.contains(x,y)) grab = true;
		else if(lockButton.contains(x,y)) lock = true;
		
		else if(controlPad.contains(point))
		{
			Vector2 posOnPad = point.cpy().sub(new Vector2(controlPad.x, controlPad.y));
			float dist = posOnPad.len()/controlPad.radius;
			Vector2 posNorm = posOnPad.nor();
			
			if(dist >= controlpadDeadzone)
			{
				float actualDist = (dist - controlpadDeadzone) / actualRange;
				
				//scale to get full range of speed between deadzone and max margin
				if(actualDist < 1)
					posNorm.scl(actualDist);
				
				controlPadPos = posNorm;
			}
		}
	}
	
	private void handleKeyboardControls()
	{
		if(Gdx.input.isKeyPressed(Keys.DOWN)) kick = true;
		if(Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.LEFT)) grab = true;
		if(Gdx.input.isKeyPressed(Keys.UP)) lock = true;
		
		//convert WASD to velocity vector
		boolean up = Gdx.input.isKeyPressed(Keys.W);
		boolean down = Gdx.input.isKeyPressed(Keys.S);
		boolean left = Gdx.input.isKeyPressed(Keys.A);
		boolean right = Gdx.input.isKeyPressed(Keys.D);
		Vector2 dir = new Vector2();
		
		if(up && !down)
			dir.y = 1;
		else if(down && !up)
			dir.y = -1;
		if(left && !right)
			dir.x = -1;
		else if(right && !left)
			dir.x = 1;
		
		//direction finding is not additive. this will blank any movement detected by
		//a touch event, so keys have to be checked before touch
		
		controlPadPos = dir.nor();
	}
	
	private void resetControlState()
	{
		kick = false;
		grab = false;
		lock = false;
		
		controlPadPos = Vector2.Zero;		
	}
			
	public void update()
	{
		resetControlState();
		
		handleKeyboardControls();
		handleTouchControls();
	}

}
