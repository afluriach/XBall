package com.electricsunstudio.xball.network;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ObjectSocketOutput extends Thread
{
	public boolean quit;
	public Socket sock;
	Queue<Object> sendQueue;
	Lock queueLock;
	ObjectOutputStream objOut;
	Gson gson;

	InetAddress addr;
	int port;

	public ObjectSocketOutput(InetAddress addr, int port) throws IOException
	{
		this.addr = addr;
		this.port = port;

		sendQueue = new LinkedList<Object>();
		queueLock = new ReentrantLock(true);

		gson = new Gson();
		
		sock = new Socket(addr, port);
		objOut = new ObjectOutputStream(sock.getOutputStream());
	}
	
	public ObjectSocketOutput(Socket sock) throws IOException
	{
		this.addr = sock.getInetAddress();
		this.port = sock.getPort();
		
		sendQueue = new LinkedList<Object>();
		queueLock = new ReentrantLock(true);

		gson = new Gson();
		
		this.sock = sock;
		objOut = new ObjectOutputStream(sock.getOutputStream());
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
						objOut.writeObject(gson.toJson(new ObjectWrapper(obj.getClass().getName(),gson.toJson(obj))));
					} catch (NotSerializableException ex){
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
