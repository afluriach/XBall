package com.electricsunstudio.xball;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import java.io.Serializable;

public class EffectSprite implements Serializable
{
    private static final long serialVersionUID = -4886420245409938668L;
    String texture;
    Vector2 pos;
    float rotation;

    transient Sprite sprite;

    public EffectSprite(String texture, Vector2 pos, float rotation) {
        this.texture = texture;
        this.pos = pos;
        this.rotation = rotation;

        sprite = Game.loadSprite(texture);
        sprite.setRotation(rotation);
        sprite.setPosition(pos.x, pos.y);
    }

    public Sprite getSprite()
    {
        return sprite;
    }

    public void setTexture(String texture)
    {
        this.texture = texture;
        Game.changeTexture(sprite, texture);
    }

    public void setPos(Vector2 pos)
    {
        sprite.setCenter(pos.x, pos.y);
        this.pos = pos;
    }

    public void setRotation(float rotation)
    {
        this.rotation = rotation;
        sprite.setRotation(rotation);
    }

    public void recreateSrite()
    {
        sprite = Game.loadSprite(texture);
        sprite.setRotation(rotation);
        sprite.setPosition(pos.x, pos.y);
    }
}
