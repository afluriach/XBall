package com.electricsunstudio.xball;

import com.electricsunstudio.xball.physics.Physics;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.electricsunstudio.xball.network.PingIntent;

import com.electricsunstudio.xball.objects.Player;
import com.electricsunstudio.xball.network.ObjectSocketOutput;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

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
    public static int teamSize = 1;
    
    public static final Class[] availableLevels = {
        Level1.class,
        BombsAway.class,
        CoolBlast.class,
        BumpyRoad.class,
        SlipperyStadium.class,
        PlusStadium.class,
        RoughPatch.class,
        ColdPatch.class
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
    public TiledMap crntMap;
    public Level crntLevel;
    public GameObjectSystem gameObjectSystem;
    public Physics physics;
    public Random rand;

    float updateDelta = 0;
    ReentrantLock engineLock;
    
    public Controls controls;
    public Player crntPlayer;
    
    //network
    public static ObjectSocketOutput serverOutput;
    public static ObjectSocketInput serverInput;
    public static String username;
    
    boolean pingOut;
    float lastPing;
    long pingSentTime;
    String latencyStr;
    
    long seed;
    
    public Game(long seed)
    {
        this.seed = seed;
    }
    
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
    
    public void createServer()
    {
        inst = this;
        
        engine = new CoreEngine(true);
        engine.initLevel(level, seed);
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
        
        engine = new CoreEngine(false);
        engine.initLevel(level, seed);

        mapRenderer = new OrthogonalTiledMapRenderer(crntMap);

        if(physicsRender)
            physics.initRender();
        
        if(player == null)
            player = crntLevel.getPlayerName();
        crntPlayer = gameObjectSystem.getObjectByName(player, Player.class);
        if(serverInput != null)
        {
            //add listener for control data
            serverInput.addHandler(ControlState.class, new ControlHandler());
            serverInput.addHandler(PingIntent.class, new PingHandler());
            serverInput.addHandler(GameState.class, new GameStateHandler());
            lastPing = 0f;
            pingOut = false;
        }
        
        engineLock = new ReentrantLock();
    }
    
    class PingHandler implements Handler
    {
        @Override
        public void onReceived(Object t) {
            latencyStr = (int) (System.currentTimeMillis() - pingSentTime) + " ms";
            pingOut = false;
            lastPing = 0f;
        }
    }
    
    class ControlHandler implements Handler
    {
        @Override
        public void onReceived(Object t) {
            ControlState cs = (ControlState)t;
            engineLock.lock();
            engine.addControlStateToBuffer(cs);
            if(engine.playersNameMap.containsKey(cs.player) && crntPlayer != engine.playersNameMap.get(cs.player))
                engine.playerControlState.put(engine.playersNameMap.get(cs.player), cs);
            engineLock.unlock();
        }
    }
    
    class GameStateHandler implements Handler
    {
        @Override
        public void onReceived(Object t) {
            GameState state = (GameState)t;
            System.out.println("received game state, frame " + state.frameNum);
            engineLock.lock();
            restoreStateAndFastForward(state);
            engineLock.unlock();
        }
    }
    
    public void update()
    {
        updateDelta += Gdx.graphics.getDeltaTime();
        while(updateDelta >= SECONDS_PER_FRAME)
        {
            updateDelta -= SECONDS_PER_FRAME;
            updateTick();
        }
    }
    
    public GameState getState()
    {
        return new GameState(engine.crntFrame, gameObjectSystem.getState(), crntLevel.getState());
    }
    
    void restoreState(GameState s)
    {
        crntLevel.restoreFromState(s.levelState);
        gameObjectSystem.restoreFromState(s.objectState);
        engine.crntFrame = s.frameNum;
    }
    
    void restoreStateAndFastForward(GameState s)
    {
    	System.out.printf("Restore and fast forward from %d to %d\n", s.frameNum, engine.crntFrame);
        //fast forward back to the current frame after loading state
        int frameBeforeLoad = engine.crntFrame;
        
        crntLevel.restoreFromState(s.levelState);
        gameObjectSystem.restoreFromState(s.objectState);
        engine.crntFrame = s.frameNum;

        while(engine.crntFrame < frameBeforeLoad)
        {
            engine.fastForwardTick();
        }
        engine.clearControlBuffer(s.frameNum);
    }
    
    //for capturing current game state
    void saveStateToFile(Object s)
    {
        File f = new File("state.bin");
        if(!f.exists())
        {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Game.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }

        BufferedOutputStream buf = null;
        try {
            buf = new BufferedOutputStream(new FileOutputStream(f));
        } catch (FileNotFoundException ex) {
        }
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(buf);
            os.writeObject(s);
        } catch (IOException ex) {
            System.out.println("io error writing state");
            ex.printStackTrace();
        }
        try {
            os.flush();
            os.close();
            buf.flush();
            buf.close();
        } catch (IOException ex) {
            System.out.println("io error closing stream");
        }
        System.out.println("state written");
    }
    
    Object loadStateFromFile()
    {
        File f = new File("state.bin");
        Object o = null;
        if(!f.exists())
            System.out.println("state doesn't exist");
        else
        {
            BufferedInputStream buf = null;
            try {
                buf = new BufferedInputStream(new FileInputStream(f));
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            ObjectInputStream is = null;
            try {
                is = new ObjectInputStream(buf);
                o = is.readObject();
            } catch (IOException ex) {
                System.out.println("io error reading state");
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            try {
                is.close();
                buf.close();
            } catch (IOException ex) {
                System.out.println("io error closing stream");
            }
            System.out.println("state read");
        }
        return o;
    }
    
    public void updateTick()
    {
        if(Gdx.input != null)
        {
            if(Gdx.input.isKeyJustPressed(Input.Keys.F))
            {
                saveStateToFile(getState());
            }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.R))
            {
                restoreState((GameState) loadStateFromFile());
            }
        }
        
        engineLock.lock();
        
        //System.out.printf("game update tick frame " + engine.crntFrame);
        if(controls != null)
        {
            controls.update();
            ControlState state = controls.getState();
            state.player = player;
            //update current
            engine.playerControlState.put(crntPlayer, state);
            //add our state to the buffer as well
            engine.addControlStateToBuffer(state);
        }
        
        if(serverOutput != null)
        {
            if(!pingOut && lastPing >= 1)
            {
                serverOutput.send(new PingIntent());
                pingSentTime = System.currentTimeMillis();
                pingOut = true;
            }
            lastPing += Game.SECONDS_PER_FRAME;
            //System.out.printf("controlstate frame sent " + engine.playerControlState.get(crntPlayer).frameNum);
            serverOutput.send(engine.playerControlState.get(crntPlayer));
        }
        
        engine.updateTick();
        
        engineLock.unlock();
    }
    
    public void serverTick()
    {
        engine.fastForwardTick();
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
        
        engineLock.lock();
        batch.begin();
        gameObjectSystem.render(batch);
        batch.end();
        engineLock.unlock();

        if(physicsRender)
        {
            physics.debugRender(camera.combined);
        }
        
        controls.render(shapeRenderer);
        
        batch.setProjectionMatrix(defaultMatrix);
        
        crntLevel.render();
        
        if(latencyStr != null)
            drawTextLeftAlign(Color.WHITE, latencyStr, 50, screenHeight-50);
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
        if(Gdx.app == null || Gdx.files == null) return null;
        
        Texture texture = new Texture(Gdx.files.internal("sprite/"+name+".png"));
        Sprite sprite = new Sprite(texture);
        
        sprite.setOriginCenter();
        
        return sprite;
    }
    
    public static Sprite loadSprite(String name, Vector2 pos, float scale)
    {
        Sprite s = loadSprite(name);
        s.setCenter(pos.x, pos.y);
        s.setScale(scale);
        return s;
    }
    
    public static void changeTexture(Sprite sprite, String name)
    {
        if(Gdx.app == null || Gdx.files == null) return;
        sprite.getTexture().dispose();
        sprite.setTexture(new Texture(Gdx.files.internal("sprite/"+name+".png")));
    }
        
    public static Vector2 mapObjectPos(MapObject mo)
    {
        Rectangle rect = ((RectangleMapObject)mo).getRectangle();
        return rect.getCenter(new Vector2()).scl(Game.TILES_PER_PIXEL);
    }
    
    public static void log(String msg) {
        if(Gdx.app == null)
            System.out.println(tag + ": " + msg);
        else
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
    
    public static String getMapName(Class levelCls)
    {
        try {
            Field f = levelCls.getField("mapName");
            return (String) f.get(null);
        } catch (SecurityException ex) {
            log("class " + levelCls.getSimpleName() + ", name is not accessible");
            return null;
        } catch(IllegalAccessException ex){
            log("class " + levelCls.getSimpleName() + " illegal access");
            return null;
        } catch (IllegalArgumentException ex) {
            return null;
        } catch (NoSuchFieldException ex) {
            return null;
        }
    }
    
    public static String levelName(Class levelCls)
    {
        try {
            Field f = levelCls.getField("name");
            return (String) f.get(null);
        } catch (Exception ex) {
            throw new RuntimeException("Level " + levelCls.getSimpleName() + " does not have a name");
        }
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