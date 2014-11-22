package com.electricsunstudio.xball;

import com.electricsunstudio.xball.objects.Player;

/**
 *
 * @author toni
 */
public interface HitListener
{
    public void onHit(Player player, HitType type);
}
