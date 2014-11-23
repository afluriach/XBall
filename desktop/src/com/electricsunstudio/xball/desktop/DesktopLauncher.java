package com.electricsunstudio.xball.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.electricsunstudio.xball.Game;

public class DesktopLauncher {
    public static void main (String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "X-Ball";
        config.width = 1280;
        config.height = 720;
        new LwjglApplication(new Game(System.currentTimeMillis()), config);
    }
}
