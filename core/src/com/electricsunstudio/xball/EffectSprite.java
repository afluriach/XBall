package com.electricsunstudio.xball;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

import java.io.Serializable;

public class EffectSprite implements Serializable
{
    private static final long serialVersionUID = -4886420245409938668L;
    String texture;
    Vector2 pos;
    float rotation;
    //to display the effect offset from an entity in the given direction
    Vector2 offsetDir;
    float entityRadius;

    transient Sprite sprite;

    public EffectSprite(String texture, Vector2 pos, float rotation) {
        this.texture = texture;
        this.pos = pos;
        this.rotation = rotation;

        if(Gdx.app != null)
        {
	        sprite = Game.loadSprite(texture);
	        sprite.setRotation(rotation);
	        sprite.setPosition(pos.x, pos.y);
        }
    }

    public Sprite getSprite()
    {
        return sprite;
    }

    public void setTexture(String texture)
    {
        this.texture = texture;
        if(Gdx.app != null)
        	Game.changeTexture(sprite, texture);
    }

    public void setPos(Vector2 pos)
    {
        this.pos = pos;
        
        if(Gdx.app != null)
        	updateSpritePos();
    }
    
    public void setEntityOffset(Vector2 dir, float radius)
    {
    	offsetDir = dir;
    	entityRadius = radius;
    	
    	if(Gdx.app != null)
    		updateSpritePos();
    }
    
    void updateSpritePos()
    {
    	Vector2 offset = offsetDir == null ? Vector2.Zero : offsetDir.cpy().scl(entityRadius+sprite.getHeight()/2);
    	sprite.setCenter(offset.x+pos.x, offset.y+pos.y);
    }

    public void setRotation(float rotation)
    {
        this.rotation = rotation;
        
        if(Gdx.app != null)
        	sprite.setRotation(rotation);
    }

    public void recreateSprite()
    {
    	if(Gdx.app == null) return;
    	
        sprite = Game.loadSprite(texture);
        sprite.setRotation(rotation);
        updateSpritePos();
    }
    
    public void setAlpha(float a)
    {
    	if(Gdx.app != null)
    		sprite.setAlpha(a);
    }
}
