package com.electricsunstudio.xball;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import java.util.ArrayList;

public class ObjectsInSpaceCallback implements QueryCallback
{
	public ArrayList<GameObject> detected = new ArrayList();
	
	public ObjectsInSpaceCallback()
	{
	}

	@Override
	public boolean reportFixture(Fixture fixture)
	{
		GameObject go = (GameObject)fixture.getBody().getUserData();
		if(go != null && !fixture.isSensor()){
			detected.add(go);
		}
		return true;
	}

}
