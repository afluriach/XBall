package com.electricsunstudio.xball;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

public class ContactHandler implements ContactListener
{
	@Override
	public void beginContact(Contact contact)
	{
		GameObject a = (GameObject) contact.getFixtureA().getBody().getUserData();
		GameObject b = (GameObject) contact.getFixtureB().getBody().getUserData();

		//wall objects do not have a GameObject reference
		
		if(contact.isTouching() && a != null && b != null)
		{
			a.handleContact(b);
			b.handleContact(a);
		}
	}

	@Override
	public void endContact(Contact contact)
	{
		//after destroying a body, endcontact will be handled with a null fixture.
		//instead, handle endcontact when gameobject is removed from object system.
		if(contact.getFixtureA() == null || contact.getFixtureB() == null)
			return;
		
		Body bodyA = contact.getFixtureA().getBody();
		Body bodyB = contact.getFixtureB().getBody();
		
		//body may have belonged to an expired object and might be null
		
		
		GameObject a = (GameObject) bodyA.getUserData();
		GameObject b = (GameObject) bodyB.getUserData();
		
		if(a != null && b != null)
		{
			a.handleEndContact(b);
			b.handleEndContact(a);
		}
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

}
