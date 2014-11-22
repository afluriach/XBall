package com.electricsunstudio.xball;

import com.electricsunstudio.xball.physics.Physics;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.electricsunstudio.xball.levels.*;
import com.electricsunstudio.xball.network.Handler;
import com.electricsunstudio.xball.network.ObjectSocketInput;

import com.electricsunstudio.xball.objects.Player;
import com.electricsunstudio.xball.network.ObjectSocketOutput;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

public class Game extends ApplicationAdapter {
    public static final int PIXELS_PER_TILE = 64;
    public static final float TILES_PER_PIXEL = (float) (1.0/PIXELS_PER_TILE);
    
    public static final int FRAMES_PER_SECOND = 60;
    public static final float SECONDS_PER_FRAME = (float) 1.0/FRAMES_PER_SECOND;
    
    public static final float latencyUpdateInterval = 0.5f;
    
    public static final boolean physicsRender = true;
    
    public static final String tag = "X-Ball";
    
    public static Class level = Level1.class;
    public static String player;
    
    public static final Class[] availableLevels = {
        Level1.class,
        BombVoyage.class,
        BombsAway.class,
        CoolBlast.class,
        BumpyRoad.class,
        SlipperyStadium.class
    };

    public static Game inst;
    public CoreEngine engine;
    
    OrthogonalTiledMapRenderer mapRenderer;
    
    OrthographicCamera camera;
    public int screenWidth, screenHeight;
    
    public SpriteBatch batch;
    public SpriteBatch guiBatch;
    public ShapeRenderer shapeRenderer;
    public BitmapFont font;
    
    //these are owned by the engine but a reference will be
    //left here for convienence
    TiledMap crntMap;
    public Level crntLevel;
    public GameObjectSystem gameObjectSystem;
    public Physics physics;
    public Random rand;

    float updateDelta = 0;
    
    public Controls controls;
    public Player crntPlayer;
    HashMap<Player,ControlState> playerControlState = new HashMap<Player, ControlState>();
    HashMap<String,Player> playersNameMap = new HashMap<String, Player>();
    
    //network
    public static ObjectSocketOutput serverOutput;
    public static ObjectSocketInput serverInput;
    public static String username;
    
    TreeMap<Integer, Long> controlTimesSent;
    int[] currentLatencies;
    int numLatencies;
    String latencyStr;
    
    void initCamera()
    {
        camera = new OrthographicCamera(screenWidth, screenHeight);
        camera.position.set(screenWidth/2, screenHeight/2, 0);
        camera.update();
    }
    
    void setCameraPosition(Vector2 pos)
    {
        Vector2 pixelSpace = pos.cpy().scl(PIXELS_PER_TILE);
        camera.position.set(pixelSpace.x, pixelSpace.y, 0);
        camera.update();
        mapRenderer.setView(camera);
    }
    
    @Override
    public void create () {
        inst = this;
        
        controls = new Controls();
        
        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();
        
        initCamera();
        
        batch = new SpriteBatch();
        guiBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont(Gdx.files.internal("font/arial-32.fnt"));
        
        engine = new CoreEngine();
        engine.initLevel(level, System.currentTimeMillis());

        mapRenderer = new OrthogonalTiledMapRenderer(crntMap);
        
        if(player == null)
            player = crntLevel.getPlayerName();
        crntPlayer = gameObjectSystem.getObjectByName(player, Player.class);
        
        //init map for player name map
        for(Player p : gameObjectSystem.getObjectsByType(Player.class))
        {
            playersNameMap.put(p.getName(), p);
        }
        
        //init players control state
        for(Player p : playersNameMap.values())
        {
            playerControlState.put(p, new ControlState());
        }
        
        if(serverInput != null)
        {
            //add listener for control data
            serverInput.addHandler(ControlState.class, new ControlHandler());
            controlTimesSent = new TreeMap<Integer, Long>();
            currentLatencies = new int[(int) (Game.FRAMES_PER_SECOND*latencyUpdateInterval)];
            numLatencies = 0;
        }
    }
    
    class ControlHandler implements Handler
    {
        @Override
        public void onReceived(Object t) {
            ControlState cs = (ControlState)t;
            playerControlState.put(playersNameMap.get(cs.player), cs);
            
            if(cs.player.equals(player))
            {
                //one of our own control states. use it to measure ping
                if(!controlTimesSent.containsKey(cs.frameNum))
                {
                    //control packets should not be received out of sequence, but it ocasionally happens and they will be ignored
                    System.out.printf("controlstate response for non-existant frame %d, min frame in buffer is %d and max is %d\n.", cs.frameNum, controlTimesSent.firstKey(), controlTimesSent.lastKey());
                }
                else
                {
                    if(numLatencies < currentLatencies.length)
                    {
                        currentLatencies[numLatencies] = (int) (System.currentTimeMillis() - controlTimesSent.get(cs.frameNum));
                        ++numLatencies;
                    }
                    
                    //remove older records - this crash didn't make sense, how can controlTimesSent be empty if it contains the received frame number?
                    while(!controlTimesSent.isEmpty() && controlTimesSent.firstKey() < cs.frameNum)
                        controlTimesSent.remove(controlTimesSent.firstKey());
                }
            }
        }
    }
    
    public void update()
    {
        controls.update();
        controls.updateState(playerControlState.get(crntPlayer));
        
        if(serverOutput != null)
        {
            serverOutput.send(playerControlState.get(crntPlayer));
            controlTimesSent.put(engine.crntFrame, System.currentTimeMillis());
        }
        updateDelta += Gdx.graphics.getDeltaTime();
        while(updateDelta >= SECONDS_PER_FRAME)
        {
            updateDelta -= SECONDS_PER_FRAME;
            updateTick();
        }
    }
    
    public void updateTick()
    {
        for(Entry<Player,ControlState> e : playerControlState.entrySet())
        {
            e.getKey().handleControls(e.getValue());
        }
        engine.updateTick();
    }
    
    @Override
    public void render () {
        update();
        
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT);
        
        setCameraPosition(crntPlayer.getCenterPos());
        
        Matrix4 defaultMatrix = batch.getProjectionMatrix();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        //batch.draw(img, 0, 0);
        mapRenderer.render();
        batch.end();
        
        batch.begin();
        gameObjectSystem.render(batch);
        batch.end();

        if(physicsRender)
        {
            physics.debugRender(camera.combined);
        }
        
        controls.render(shapeRenderer);
        
        batch.setProjectionMatrix(defaultMatrix);
        
        crntLevel.render();
        
        updateLatency();
        if(latencyStr != null)
            drawTextLeftAlign(Color.WHITE, latencyStr, 50, screenHeight-50);
    }
    
    void updateLatency()
    {
        if(currentLatencies != null && numLatencies == currentLatencies.length)
        {
            int min=Integer.MAX_VALUE, max=0, sum=0;
            
            for(Integer i : currentLatencies)
            {
                sum += i;
                if(i < min) min = i;
                if(i > max) max = i;
            }
            
            latencyStr = String.format("%d,%d,%d ms", min, max, (int)(sum/currentLatencies.length));
            numLatencies = 0;
        }
    }
    
    public static void drawSprite(Sprite sprite, Vector2 centerPos, SpriteBatch batch, float rotation)
    {
        Vector2 pix = centerPos.cpy().scl(Game.PIXELS_PER_TILE);
        
        sprite.setCenter(pix.x, pix.y);
        sprite.setRotation(rotation);
        
        sprite.draw(batch);
    }
    
    public static void drawSprite(Sprite sprite, SpriteBatch batch)
    {
        sprite.draw(batch);
    }
    
    public static Sprite loadSprite(String name)
    {
        Texture texture = new Texture(Gdx.files.internal("sprite/"+name+".png"));
        Sprite sprite = new Sprite(texture);
        
        sprite.setOriginCenter();
        
        return sprite;
    }
    
    public static void changeTexture(Sprite sprite, String name)
    {
        sprite.getTexture().dispose();
        sprite.setTexture(new Texture(Gdx.files.internal("sprite/"+name+".png")));
    }
        
    public static Vector2 mapObjectPos(MapObject mo)
    {
        Rectangle rect = ((RectangleMapObject)mo).getRectangle();
        return rect.getCenter(new Vector2()).scl(Game.TILES_PER_PIXEL);
    }
    
    public static void log(String msg) {
        Gdx.app.log(tag, msg);
    }
    
    public static String string(Vector2 v)
    {
        return v.x + ", " + v.y;
    }

        /**
     *
     * @param h [0.0 to 360.0)
     * @param s [0.0 to 1.0]
     * @param v [0.0 to 1.0]
     * @param a [0.0 to 1.0]
     * @return
     */
    public static Color hsva(float h, float s, float v, float a)
    {
        float r1, g1, b1;
        float C = v*s;
        float hPrime = h / 60;
        float x = C*(1-Math.abs(hPrime % 2.0f - 1));
        float m = v - C;
        
        if(s == 0)
        {
            //hue is undefined and no color will be added
            r1 = g1 = b1 = 0;
        }
        else if(0 <= hPrime && hPrime < 1)
        {
            r1 = C;
            g1 = x;
            b1 = 0;
        }
        else if(1 <= hPrime && hPrime < 2)
        {
            r1 = x;
            g1 = C;
            b1 = 0;
        }
        else if(2 <= hPrime && hPrime < 3)
        {
            r1 = 0;
            g1 = C;
            b1 = x;
        }
        else if(3 <= hPrime && hPrime < 4)
        {
            r1 = 0;
            g1 = x;
            b1 = C;
        }
        else if(4 <= hPrime && hPrime < 5)
        {
            r1 = x;
            g1 = 0;
            b1 = C;
        }
        else if(5 <= hPrime && hPrime < 6)
        {
            r1 = C;
            g1 = 0;
            b1 = x;
        }
        else
        {
            throw new IllegalArgumentException(String.format("Illegal hue given: %f", h));
        }
        
        return new Color(r1+m, g1+m, b1+m, a);
    }

    public void drawTextCentered(Color color, String msg, float x, float y)
    {
        float lineWidth = font.getBounds(msg).width;
        
        guiBatch.begin();
        font.setScale(1f);
        font.setColor(color);
        font.draw(guiBatch, msg, x-lineWidth/2, y+font.getCapHeight()/2);
        guiBatch.end();
    }
    
    public void drawTextLeftAlign(Color color, String msg, float x, float y)
    {
        guiBatch.begin();
        font.setScale(1f);
        font.setColor(color);
        font.draw(guiBatch, msg, x, y);
        guiBatch.end();
    }
    
    public static Rectangle toTilespace(Rectangle r)
    {
        Rectangle rect = new Rectangle();
        rect.x = r.x *TILES_PER_PIXEL;
        rect.y = r.y *TILES_PER_PIXEL;
        rect.width = r.width *TILES_PER_PIXEL;
        rect.height = r.height *TILES_PER_PIXEL;
        
        return rect;
    }
    
    public static Vector2 rayRad(double len, double angle)
    {
        return new Vector2((float) (len*Math.cos(angle)), (float)(len*Math.sin(angle)));
    }
    
    public boolean onMapLayer(String layer, Vector2 pos)
    {
        TiledMapTileLayer tileLayer = (TiledMapTileLayer) crntMap.getLayers().get(layer);
        
        if(tileLayer == null) return false;
        
        return tileLayer.getCell((int)pos.x, (int)pos.y) != null;
    }
    
    public static String[] availableLevelNames()
    {
        String[] names = new String[availableLevels.length];
        
        for(int i=0; i< availableLevels.length; ++i )
        {
            Class cls = availableLevels[i];

            try {
                Field f = cls.getField("name");
                names[i] = (String) f.get(null);
            } catch (NoSuchFieldException ex) {
                log("class " + cls.getSimpleName() + " does not have name");
                names[i] = "";
            } catch (SecurityException ex) {
                log("class " + cls.getSimpleName() + ", name is not accessible");
                names[i] = "";
            } catch(IllegalAccessException ex){
                log("class " + cls.getSimpleName() + " illegal access");
                names[i] = "";
            }
        }
        return names;
    }
    
    public static Class getLevelFromSimpleName(String name)
    {
        try {
            return Class.forName("com.electricsunstudio.xball.levels." + name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}