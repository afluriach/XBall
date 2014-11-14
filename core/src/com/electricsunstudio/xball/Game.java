package com.electricsunstudio.xball;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Game extends ApplicationAdapter {
	public static final int PIXELS_PER_TILE = 64;
	public static final float TILES_PER_PIXEL = (float) (1.0/PIXELS_PER_TILE);
	
	public static final int FRAMES_PER_SECOND = 30;
	public static final float SECONDS_PER_FRAME = (float) 1.0/FRAMES_PER_SECOND;
	
	public static Game inst;
	
	TmxMapLoader mapLoader;
	OrthogonalTiledMapRenderer mapRenderer;
	
	OrthographicCamera camera;
	int screenWidth, screenHeight;
	
	SpriteBatch batch;
	//Texture img;
	
	TiledMap crntMap;
	
	public GameObjectSystem gameObjectSystem;
	public Physics physics;
	
	void initCamera()
	{
		camera = new OrthographicCamera(screenWidth, screenHeight);
		camera.position.set(screenWidth/2, screenHeight/2, 0);
		camera.update();
		mapRenderer.setView(camera);
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
		crntMap = mapLoader.load("map/stadium1.tmx");
		mapRenderer = new OrthogonalTiledMapRenderer(crntMap);
		
		gameObjectSystem = new GameObjectSystem();
		physics = new Physics();
		
		screenWidth = Gdx.graphics.getWidth();
		screenHeight = Gdx.graphics.getHeight();
		
		initCamera();
		
		batch = new SpriteBatch();
		//img = new Texture("badlogic.jpg");
		setCameraPosition(new Vector2(8,6));
		
		loadMapObjects();
		gameObjectSystem.handleAdditions();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT);
		
		Matrix4 defaultMatrix = batch.getProjectionMatrix();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		//batch.draw(img, 0, 0);
		mapRenderer.render();
		batch.end();
		
		batch.begin();
		gameObjectSystem.render(batch);
		batch.end();
		
		batch.setProjectionMatrix(defaultMatrix);
	}
	
	public static void drawTexture(Texture texture, Vector2 centerPos, SpriteBatch batch)
	{
		int centerPixX = (int) (centerPos.x*Game.PIXELS_PER_TILE);
		int centerPixY = (int) (centerPos.y*Game.PIXELS_PER_TILE);

		//lower left corner
		int x = centerPixX - texture.getWidth()/2;
		int y = centerPixY - texture.getHeight()/2;
		
		batch.draw(texture, x, y, texture.getWidth(), texture.getHeight());
	}
	
	public static Texture loadSprite(String name)
	{
		return new Texture(Gdx.files.internal("sprite/"+name+".png"));
	}
	
	public void loadMapObjects()
	{
		loadObjectsFromLayer(crntMap.getLayers().get("agents"));
	}
	
	public void loadObjectsFromLayer(MapLayer layer)
	{
		log("loading layer " + layer.getName());
		
		for(MapObject mo : layer.getObjects())
		{
			GameObject obj = GameObject.instantiate(mo);
			
			gameObjectSystem.addObject(obj);
			
			log(String.format("loaded object %s, %s at %s", obj.getName(), obj.getClass().getName(), Game.string(obj.getCenterPos())));
		}
	}
	
	public static Vector2 mapObjectPos(MapObject mo)
	{
		Vector2 pos = new Vector2(mo.getProperties().get("x", Float.class),
								  mo.getProperties().get("y", Float.class));
		pos.scl(Game.TILES_PER_PIXEL);
		return pos;
	}
	
	static void log(String msg) {
		Gdx.app.log("X-Ball", msg);
	}
	
	static String string(Vector2 v)
	{
		return v.x + ", " + v.y;
	}

}
