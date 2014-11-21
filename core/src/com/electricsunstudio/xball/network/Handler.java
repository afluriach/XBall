package com.electricsunstudio.xball.network;

/**
 *
 * @author toni
 */
public interface Handler<T> {
	public <T> void onReceived(T t);
}
