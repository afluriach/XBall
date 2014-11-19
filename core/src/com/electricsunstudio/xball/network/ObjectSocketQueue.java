package com.electricsunstudio.xball.network;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObjectSocketQueue extends Thread
{
	public boolean quit;
	Socket sock;
	Queue<Object> sendQueue;
	Lock queueLock;
	ObjectOutputStream objOut;
	Gson gson;

	InetAddress addr;
	int port;

	public ObjectSocketQueue(InetAddress addr, int port)
	{
		this.addr = addr;
		this.port = port;

		sendQueue = new LinkedList<Object>();
		queueLock = new ReentrantLock(true);

		gson = new Gson();
	}

	public void send(Object obj)
	{
		queueLock.lock();
		try{
			sendQueue.add(obj);
		} finally{
			queueLock.unlock();
		}
	}

	@Override
	public void run()
	{
		try {
			sock = new Socket(addr, port);
			objOut = new ObjectOutputStream(sock.getOutputStream());
		} catch (IOException ex) {
			ex.printStackTrace();
			quit = true;
		}

		while(!quit && !sock.isClosed() && sock.isConnected())
		{
			//try to empty queue before waiting
			while(!sendQueue.isEmpty())
			{
				Object obj = null;
				queueLock.lock();
				try{
					if(!sendQueue.isEmpty())
						obj = sendQueue.remove();
				} finally{
					queueLock.unlock();
				}

				if(obj != null)
				{
					try {
						objOut.writeObject(gson.toJson(obj));
					} catch (NotSerializableException ex){
//							Toast.makeText(ConnectToServer.this, obj.getClass().getSimpleName() + " is not serializable", Toast.LENGTH_SHORT).show();
						ex.printStackTrace();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		
		if(!sock.isClosed())
			try {
				sock.close();
		} catch (IOException ex) {
				System.out.println("excpetion closing server thread");
		}
	}
}
