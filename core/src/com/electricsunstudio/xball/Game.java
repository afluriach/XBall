package com.electricsunstudio.xball;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Game extends ApplicationAdapter {
	static final int PIXELS_PER_TILE = 64;
	
	TmxMapLoader mapLoader;
	OrthogonalTiledMapRenderer mapRenderer;
	
	OrthographicCamera camera;
	int screenWidth, screenHeight;
	
	SpriteBatch batch;
	//Texture img;
	
	TiledMap crntMap;
	
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
		mapLoader = new TmxMapLoader();
		crntMap = mapLoader.load("map/stadium1.tmx");
		mapRenderer = new OrthogonalTiledMapRenderer(crntMap);
		
		screenWidth = Gdx.graphics.getWidth();
		screenHeight = Gdx.graphics.getHeight();
		
		initCamera();
		
		batch = new SpriteBatch();
		//img = new Texture("badlogic.jpg");
		setCameraPosition(new Vector2(3,2));
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
		batch.setProjectionMatrix(defaultMatrix);
	}
}
