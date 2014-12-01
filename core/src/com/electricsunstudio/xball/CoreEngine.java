package com.electricsunstudio.xball;

//to be run by both client and server. can be run headless

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.files.FileHandle;
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
import java.util.TreeMap;

public class CoreEngine {
    public static final int PIXELS_PER_TILE = Game.PIXELS_PER_TILE;
    public static final float TILES_PER_PIXEL = Game.TILES_PER_PIXEL;
    
    public static final int FRAMES_PER_SECOND = Game.FRAMES_PER_SECOND;
    public static final float SECONDS_PER_FRAME = Game.SECONDS_PER_FRAME;
    
    TmxMapLoader mapLoader;
    ServerMapLoader serverMapLoader;
    
    public TiledMap crntMap;
    Level crntLevel;

    public GameObjectSystem gameObjectSystem;
    public Physics physics;

    public Random rand;
    public int crntFrame = 0;
    
    long seed;
    
    HashMap<Player,ControlState> playerControlState;
    TreeMap<Integer, HashMap<Player,ControlState>> pastControlStates;
    HashMap<String,Player> playersNameMap;
    
    public CoreEngine(boolean server)
    {
        if(!server)
            mapLoader = new TmxMapLoader();
        else
            serverMapLoader = new ServerMapLoader();
    }
    
    public void initLevel(Class level, long seed)
    {
        this.seed = seed;
        
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
        pastControlStates = new TreeMap<Integer, HashMap<Player, ControlState>>();
        initControlBuffer();
    }
    
    //initialize the buffer with the starting state, which includes
    //entries for all players.
    public void initControlBuffer()
    {
        pastControlStates.put(crntFrame, (HashMap) playerControlState.clone());
    }
    
    //entries will get added to the buffer when they become available. this 
    //players state will always be up to date.
    public void addControlStateToBuffer(ControlState s)
    {
        if(!pastControlStates.containsKey(s.frameNum))
        {
            pastControlStates.put(s.frameNum, new HashMap());
        }
        Player p = playersNameMap.get(s.player);
        pastControlStates.get(s.frameNum).put(p, s);
    }
    
    public void updateTick()
    {
        rand.setSeed(seed+crntFrame);
        
        handleControls();
        
        crntLevel.update();
        gameObjectSystem.update();
        physics.update();

        ++crntFrame;
    }
    
    public void fastForwardTick()
    {
        rand.setSeed(seed+crntFrame);
        
        applyPastControls();
        
        crntLevel.update();
        gameObjectSystem.update();
        physics.update();

        ++crntFrame;
    }
    
    public void clearControlBuffer(int limitFrame)
    {
		while(!pastControlStates.isEmpty() && pastControlStates.firstKey() < limitFrame)
		{
			pastControlStates.remove(pastControlStates.firstKey());
		}
    }
    
    void applyPastControls()
    {
        HashMap<Player,ControlState> nextFrameState = getNextFrameControls();
        
        System.out.println("applying past controls for frame " + crntFrame);
        
        //apply the past controls stored for the current frame
        for(Map.Entry<Player,ControlState> e : pastControlStates.get(crntFrame).entrySet())
        {
            e.getKey().handleControls(e.getValue());

            //if the next frame does have an entry for this player, push the
            //current frames entry to the next one
            if(!nextFrameState.containsKey(e.getKey()))
            {
                //the control state will still the contain the actual frame when
                //it was captured, but this will not affect how it is processed
                nextFrameState.put(e.getKey(), e.getValue());
            }
        }
    }
    
    HashMap<Player,ControlState> getNextFrameControls()
    {
        if(!pastControlStates.containsKey(crntFrame+1))
        {
            pastControlStates.put(crntFrame+1, new HashMap());
        }
        return pastControlStates.get(crntFrame+1);
    }
    
    void handleControls()
    {
        //apply control state for each player
        //use the latest that is available for all other players
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
        
        System.out.printf("%s loaded\n", cls.getSimpleName());
        loadMap(crntLevel.getMapName());
        System.out.printf("map loaded\n");
        crntLevel.init();
        System.out.println("level init");
    }
    
    void loadMap(String name)
    {
        gameObjectSystem = new GameObjectSystem();
        physics = new Physics();
        
        if(mapLoader != null)
            crntMap = mapLoader.load("map/"+name+".tmx");
        else crntMap = serverMapLoader.load("map/"+name+".tmx");

        Game.inst.gameObjectSystem = gameObjectSystem;
        Game.inst.physics = physics;
        Game.inst.crntMap = crntMap;
        
        loadMapObjects();
        gameObjectSystem.handleAdditions();

        addWalls();
    }
    
    public void loadMapObjects()
    {
        for(int i=0;i<Game.teamSize; ++i)
        {
            loadObjectsFromLayer(crntMap.getLayers().get("players"+(i+1)));
        }
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
