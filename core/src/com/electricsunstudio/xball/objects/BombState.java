package com.electricsunstudio.xball.objects;

import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.GameObject;
import com.electricsunstudio.xball.GameObjectState;
import static com.electricsunstudio.xball.objects.Bomb.partialTime;

public class BombState extends GameObjectState
{
    private static final long serialVersionUID = -317815928672504752L;
    public float fuse;
    public BombState(Bomb b)
    {
        super(b);
        fuse = b.fuseRemaining;
    }

    public void applyState(Bomb b)
    {
        super.applyState(b);
        b.fuseRemaining = fuse;

        if(b.fuseRemaining > Bomb.partialTime)
            b.spriteState = Bomb.SpriteState.normal;
        else if(b.fuseRemaining > Bomb.imminentTime)
            b.spriteState = Bomb.SpriteState.partial;
        else
            b.spriteState = Bomb.SpriteState.imminent;

        String texture = null;

        switch(b.spriteState)
        {
            case normal:
                texture = "bomb"; break;
            case partial:
                texture = "bomb_partial"; break;
            case imminent:
                texture = "bomb_imminent"; break;
        }
        Game.changeTexture(b.sprite, texture);
    }
}
