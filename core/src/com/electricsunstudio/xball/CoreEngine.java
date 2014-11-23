package com.electricsunstudio.xball;

//to be run by both client and server. can be run headless

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import static com.electricsunstudio.xball.Game.log;
import com.electricsunstudio.xball.levels.Level;
import com.electricsunstudio.xball.objects.Player;
import com.electricsunstudio.xball.physics.FilterClass;
import com.electricsunstudio.xball.physics.Physics;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CoreEngine {
    public static final int PIXELS_PER_TILE = Game.PIXELS_PER_TILE;
    public static final float TILES_PER_PIXEL = Game.TILES_PER_PIXEL;
    
    public static final int FRAMES_PER_SECOND = Game.FRAMES_PER_SECOND;
    public static final float SECONDS_PER_FRAME = Game.SECONDS_PER_FRAME;
    
    TmxMapLoader mapLoader;

    public TiledMap crntMap;
    Level crntLevel;

    public GameObjectSystem gameObjectSystem;
    public Physics physics;

    public Random rand;
    int crntFrame = 0;
    
    HashMap<Player,ControlState> playerControlState;
    HashMap<String,Player> playersNameMap;
    
    public CoreEngine()
    {
        mapLoader = new TmxMapLoader();
    }
    
    public void initLevel(Class level, long seed)
    {
        rand = new Random(seed);
        if(Game.inst != null)
            Game.inst.rand = rand;
        loadLevel(level);
        
        //init map for player name map
        playersNameMap = new HashMap<String, Player>();
        for(Player p : gameObjectSystem.getObjectsByType(Player.class))
        {
            playersNameMap.put(p.getName(), p);
        }
        
        //init players control state
        playerControlState = new HashMap<Player, ControlState>();
        for(Player p : playersNameMap.values())
        {
            playerControlState.put(p, new ControlState());
        }

    }
    
    public void updateTick()
    {
        ++crntFrame;
        
        crntLevel.update();
        
        gameObjectSystem.update();
        physics.update();
        
        handleControls();
    }
    
    void handleControls()
    {
        //apply control state for each player
        //may assume that all control states have arrived prior to processing them
        //or if a control state is not available for this frame, do not process
        //controls for that player
        for(Map.Entry<Player,ControlState> e : playerControlState.entrySet())
        {
            e.getKey().handleControls(e.getValue());
        }
    }
    
    void loadLevel(Class cls)
    {
        try {
            crntLevel = (Level) cls.getConstructor().newInstance();
        } catch (Exception ex) {
            Game.log("error loading level " + cls.getSimpleName());
            ex.printStackTrace();
            throw new RuntimeException("Error loading level");
        }
        
        if(Game.inst != null)
        {
            Game.inst.crntLevel = crntLevel;
        }
        
        loadMap(crntLevel.getMapName());
        crntLevel.init();
    }
    
    void loadMap(String name)
    {
        gameObjectSystem = new GameObjectSystem();
        physics = new Physics();
        
        crntMap = mapLoader.load("map/"+name+".tmx");

        if(Game.inst != null)
        {
            Game.inst.gameObjectSystem = gameObjectSystem;
            Game.inst.physics = physics;
            Game.inst.crntMap = crntMap;
        }
        
        loadMapObjects();
        gameObjectSystem.handleAdditions();

        addWalls();
    }
    
    public void loadMapObjects()
    {
        loadObjectsFromLayer(crntMap.getLayers().get("agents"));
        loadObjectsFromLayer(crntMap.getLayers().get("sensor"));
    }
    
    public void loadObjectsFromLayer(MapLayer layer)
    {
        log("loading layer " + layer.getName());
        
        for(MapObject mo : layer.getObjects())
        {
            GameObject obj = GameObject.instantiate(mo);
            
            gameObjectSystem.addObject(obj);
            
            log(String.format("loaded object %s, %s at %s", obj.getName(), obj.getClass().getSimpleName(), Game.string(obj.getCenterPos())));
        }
    }

    public void addWalls()
    {
        TiledMapTileLayer wallLayer = (TiledMapTileLayer) crntMap.getLayers().get("wall");
        int width = wallLayer.getWidth();
        int height = wallLayer.getHeight();

        for(int i=0;i<height; ++i)
        {
            for(int j=0;j<width; ++j)
            {
                if(wallLayer.getCell(j,  i) != null)
                {
                    physics.addRectBody(
                        new Vector2(j+0.5f,i+0.5f),
                        1f,
                        1f,
                        BodyDef.BodyType.StaticBody,
                        null,
                        1f,
                        false,
                        FilterClass.wall
                    );
                }
            }
        }
    }

}
