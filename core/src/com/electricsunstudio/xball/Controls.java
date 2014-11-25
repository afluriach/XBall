package com.electricsunstudio.xball;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import java.util.EnumMap;

public class Controls {
    public static final int maxPressEvents = 5;
    
    public static final int margin = 20;
    public static final int buttonRadius = 32;
    public static final int buttonTrim = 15;
    public static final int buttonTrimThickness = 10;
    
    public static final int controlpadDiameter = 240;
    public static final float controlpadDeadzone = 0f;
    public static final float controlpadMax = 0.4f;
    public static final float actualRange = controlpadMax - controlpadDeadzone;

    public static final float arcRadius = controlpadDiameter/2;
    //get rid of aimpad. instead just lock when blocking. and put a lock button
    //in the middle of the button circle. 
    public static final float lockButtonRadius = controlpadDiameter/2*0.4f;
    
    //screen
    int width;
    int height;
    
    public Vector2 controlPadPos = Vector2.Zero;

    Circle controlPad, actionPad;
    
    public EnumMap<Action, Boolean> state;
    EnumMap<Action, Pair<Color,Color>> actionColors;
    EnumMap<Action, Integer> actionButtonPos;
    EnumMap<Action, Integer> actionKeys;
    
    Color controlPadOuterColor = Game.hsva(251, .3f, .7f, 1f);
    Color controlPadColor = Game.hsva(251f,.1f,.3f, 1f);
    Color controlPadInnerColor = Game.hsva(251f, .1f, .8f, 1f);
        
    public Controls()
    {
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        
        state = new EnumMap<Action, Boolean>(Action.class);
        state.put(Action.kick, false);
        state.put(Action.grab, false);
        state.put(Action.special, false);
        state.put(Action.lock, false);
        
        actionColors = new EnumMap<Action, Pair<Color, Color>>(Action.class);
        actionColors.put(Action.kick, new Pair(Game.hsva(10f,.8f,.3f, 1f), Game.hsva(10f, 1f, .8f, 1f)));
        actionColors.put(Action.grab, new Pair(Game.hsva(251f,.8f,.3f, 1f), Game.hsva(251f, 1f, .8f, 1f)));
        actionColors.put(Action.special, new Pair(Game.hsva(130f,.8f,.3f, 1f), Game.hsva(130f, 1f, .8f, 1f)));
        
        actionButtonPos = new EnumMap<Action, Integer>(Action.class);
        actionButtonPos.put(Action.kick, 150);
        actionButtonPos.put(Action.grab, 270);
        actionButtonPos.put(Action.special, 30);

        actionKeys = new EnumMap<Action, Integer>(Action.class);
        actionKeys.put(Action.kick, Keys.LEFT);
        actionKeys.put(Action.grab, Keys.RIGHT);
        actionKeys.put(Action.special, Keys.UP);
        actionKeys.put(Action.lock, Keys.DOWN);

        createShapes();
    }
    
    public void createShapes()
    {
        Vector2 controlpadCenter = new Vector2(margin+controlpadDiameter/2, margin+controlpadDiameter/2);
        Vector2 actionPadCenter = new Vector2(width-margin-controlpadDiameter/2, margin+controlpadDiameter/2);

        controlPad = new Circle(controlpadCenter.x, controlpadCenter.y, controlpadDiameter/2);
        actionPad = new Circle(actionPadCenter.x, actionPadCenter.y, controlpadDiameter/2);
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

        //draw outer margin
        shapeRenderer.setColor(controlPadOuterColor);
        shapeRenderer.circle(controlPad.x, controlPad.y, controlPad.radius);
        
        shapeRenderer.setColor(controlPadColor);
        shapeRenderer.circle(controlPad.x, controlPad.y, controlPad.radius*controlpadMax);

        shapeRenderer.setColor(controlPadInnerColor);
        shapeRenderer.circle(controlPad.x, controlPad.y, controlPad.radius*controlpadDeadzone);
        
        drawArcButton(shapeRenderer, Action.kick);
        drawArcButton(shapeRenderer, Action.grab);
        drawArcButton(shapeRenderer, Action.special);
        
        shapeRenderer.setColor(state.get(Action.lock) ? controlPadOuterColor : controlPadInnerColor);
        shapeRenderer.circle(actionPad.x, actionPad.y, lockButtonRadius);
        
        shapeRenderer.end();
    }
    
    void drawArcButton(ShapeRenderer sr, Action action)
    {
        sr.setColor(state.get(action) ? actionColors.get(action).a : actionColors.get(action).b);
        sr.arc(actionPad.x, actionPad.y, arcRadius,(float) actionButtonPos.get(action), 120);
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

        if(controlPad.contains(point))
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
        else if(actionPad.contains(point))
        {
            Vector2 posOnPad = point.cpy().sub(new Vector2(actionPad.x, actionPad.y));
            float dist = posOnPad.len();

            if(dist < lockButtonRadius)
            {
                state.put(Action.lock, Boolean.TRUE);
            }
            else
            {
                //determine which section was pressed
                float angle = posOnPad.angle();
                
                for(Action a : actionButtonPos.keySet())
                {
                    float start = actionButtonPos.get(a);
                    float limit = start+120;
                    if(angle >= start && angle < limit ||
                       limit > 360 && (angle >= start || angle < limit-360))
                    {
                        state.put(a, Boolean.TRUE);
                    }
                }
            }
        }
    }
    
    private void handleKeyboardControls()
    {
        for(Action a : actionKeys.keySet())
        {
            if(Gdx.input.isKeyPressed(actionKeys.get(a)))
                state.put(a, Boolean.TRUE);
        }
        
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
        for(Action a : state.keySet())
        {
            state.put(a, Boolean.FALSE);
        }
        
        controlPadPos = Vector2.Zero;
    }
            
    public void update()
    {
        resetControlState();
        
        handleKeyboardControls();
        handleTouchControls();
    }

    public ControlState getState()
    {
        ControlState cs = new ControlState();
        cs.grab = state.get(Action.grab);
        cs.kick = state.get(Action.kick);
        cs.special = state.get(Action.special);
        cs.lock = state.get(Action.lock);
        cs.moveX = controlPadPos.x;
        cs.moveY = controlPadPos.y;
        cs.frameNum = Game.inst.engine.crntFrame;
        //System.out.printf("update controlstate frame %d, movepad %f,%f\n", cs.frameNum, cs.moveX, cs.moveY);
        return cs;
    }
}
