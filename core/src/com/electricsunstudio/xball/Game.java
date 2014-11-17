package com.electricsunstudio.xball;

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
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.electricsunstudio.xball.levels.Level;
import com.electricsunstudio.xball.levels.Level1;

import com.electricsunstudio.xball.objects.Player;
import java.util.Random;
import java.util.logging.Logger;

public class Game extends ApplicationAdapter {
	public static final int PIXELS_PER_TILE = 64;
	public static final float TILES_PER_PIXEL = (float) (1.0/PIXELS_PER_TILE);
	
	public static final int FRAMES_PER_SECOND = 30;
	public static final float SECONDS_PER_FRAME = (float) 1.0/FRAMES_PER_SECOND;
	
	public static final boolean physicsRender = true;
	
	public static Game inst;
	
	TmxMapLoader mapLoader;
	OrthogonalTiledMapRenderer mapRenderer;
	
	OrthographicCamera camera;
	public int screenWidth, screenHeight;
	
	public SpriteBatch batch;
	public SpriteBatch guiBatch;
	public ShapeRenderer shapeRenderer;
	public BitmapFont font;
	
	TiledMap crntMap;
	Level crntLevel;
	
	public GameObjectSystem gameObjectSystem;
	public Physics physics;
	
	float updateDelta = 0;
	
	public Controls controls;
	
	public Player crntPlayer;
	
	public Random rand;
	
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
		
		mapLoader = new TmxMapLoader();
		
		controls = new Controls();
		
		screenWidth = Gdx.graphics.getWidth();
		screenHeight = Gdx.graphics.getHeight();
		
		rand = new Random();
		
		initCamera();
		
		batch = new SpriteBatch();
		guiBatch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		font = new BitmapFont(Gdx.files.internal("font/arial-32.fnt"));
		
		//loadMap("stadium1");
		loadLevel(Level1.class);
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
		
		loadMap(crntLevel.getMapName());
		crntLevel.init();
	}
	
	void loadMap(String name)
	{
		gameObjectSystem = new GameObjectSystem();
		physics = new Physics();
		
		crntMap = mapLoader.load("map/"+name+".tmx");
		mapRenderer = new OrthogonalTiledMapRenderer(crntMap);
		
		loadMapObjects();
		gameObjectSystem.handleAdditions();
		
		crntPlayer = gameObjectSystem.getObjectByName("blue_player", Player.class);

		addWalls();
	}

	public void update()
	{
		controls.update();
		
		updateDelta += Gdx.graphics.getDeltaTime();
		while(updateDelta >= SECONDS_PER_FRAME)
		{
			updateDelta -= SECONDS_PER_FRAME;
			updateTick();
		}
	}
	
	public void updateTick()
	{
		crntLevel.update();
		
		gameObjectSystem.update();
		physics.update();
		
		crntPlayer.handleControls();
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
	
	public static Vector2 mapObjectPos(MapObject mo)
	{
		Rectangle rect = ((RectangleMapObject)mo).getRectangle();
		return rect.getCenter(new Vector2()).scl(Game.TILES_PER_PIXEL);
	}
	
	public static void log(String msg) {
		Gdx.app.log("X-Ball", msg);
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
						BodyType.StaticBody,
						null,
						1f,
						false,
						FilterClass.wall
					);
				}
			}
		}
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
}
