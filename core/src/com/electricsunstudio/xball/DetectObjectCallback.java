package com.electricsunstudio.xball;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;

public class DetectObjectCallback implements QueryCallback
{
	//has any object been found
	boolean detected = false;
	
	public boolean detected()
	{
		return detected;
	}
	
	public DetectObjectCallback()
	{
	}

	@Override
	public boolean reportFixture(Fixture fixture)
	{
		GameObject go = (GameObject)fixture.getBody().getUserData();
		if(!fixture.isSensor()){
			detected = true;
			return false; //object found, can terminate query.
		}
		return true;
	}

}
