package com.electricsunstudio.xball.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.electricsunstudio.xball.network.ClientAction;
import com.electricsunstudio.xball.network.ClientIntent;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerLauncher {
	public static final int serverPort = 49000;
	static final int maxPacketSize = 65536;
	
	static HashMap<String,Connection> connectedUsers = new HashMap<String, Connection>();
	//the socket that each user is using for their connection
	//may be better to map it to the server thread using to handle each user since
	//the thread owns the socket and will handle the IO
	static HashMap<String, Thread> userThreads = new HashMap<String, Thread>();
	
	static Lock userdataLock = new ReentrantLock(true);
	
	static Gson gson = new Gson();
	
	public static void main (String[] arg) {
		MainServerThread serverThread = new MainServerThread(serverPort);
		serverThread.start();
	}
	
	static class Connection
	{
		public Connection(InetAddress addr, int port) {
			this.addr = addr;
			this.port = port;
		}
		
		public InetAddress addr;
		public int port;
		
		public boolean equals(InetAddress addr, int port)
		{
			return this.addr.equals(addr) && this.port == port;
		}
		
		@Override
		public String toString()
		{
			return addr.toString() + ":" + port;
		}
	}
	
	//the main server thread that listens on the server for port incoming client
	//connections
	static class MainServerThread extends Thread
	{
		ServerSocket sock;
		boolean quit = false;
		public MainServerThread(int port)
		{
			try {
				sock = new ServerSocket(port);
			} catch (SocketException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		@Override
		public void run()
		{
			if(!sock.isClosed())
				System.out.printf("server running on %s:%d\n", sock.getInetAddress().toString(), sock.getLocalPort());
			
			while(!quit && !sock.isClosed())
			{
				try {
					Socket clientConnection = sock.accept();
					System.out.printf("Client connection received from %s:%d\n", clientConnection.getInetAddress().toString(), clientConnection.getPort());
					new ServerThread(clientConnection).start();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	//per client socket opened. listen for intents by client and communicate
	//result
	static class ServerThread extends Thread
	{
		Socket sock;
		Connection conn;
		
		String user;
		
		ObjectInputStream objIn;
		ObjectOutputStream objOut;
		
		boolean quit;
		
		public ServerThread(Socket sock)
		{
			this.sock = sock;
			conn = new Connection(sock.getInetAddress(), sock.getPort());
			
			try {
				objIn = new ObjectInputStream(sock.getInputStream());
				objOut = new ObjectOutputStream(sock.getOutputStream());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		@Override
		public void run()
		{
			if(objIn == null || objOut == null)
			{
				System.out.println("Error opening object IO on server thread.");
				return;
			}
			
			while(!sock.isClosed() && !quit)
			{
				try {
					Object obj = objIn.readObject();
										
					if(obj instanceof String)
					{
						ClientIntent intent = gson.fromJson((String)obj, ClientIntent.class);
						handleIntent(intent, conn);
					}
					else					
					{
						System.out.println("invalid object type from client");
					}
				} catch(SocketException ex){
					System.out.println("Socket has been reset.");
					quit = true;
					if(!sock.isClosed())
					{
						try {
							sock.close();
						} catch (IOException ex1) {
							ex.printStackTrace();
						}
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				} catch (ClassNotFoundException ex) {
					ex.printStackTrace();
				}
			}
		}
		
		void handleIntent(ClientIntent intent, Connection conn)
		{
			//TODO check that this socket hasn't already submitted a login request
			if(intent.action == ClientAction.connect)
			{
				user = intent.username;
				userdataLock.lock();
				try {
					connectedUsers.put(intent.username, conn);
					userThreads.put(intent.username, this);
				} finally {
					userdataLock.unlock();
				}
				System.out.println(intent.username + " is now online, " + conn.addr + ":" + conn.port);	
			}
			else if(intent.action == ClientAction.disconnect)
			{
				if(!connectedUsers.containsKey(intent.username))
				{
					System.out.println("Invalid disconnect for non-existant user " +
						intent.username + " from " + conn);
				}
				else if(userThreads.get(intent.username) != this)
				{
					System.out.printf("Invalid disconnect for %s, on wrong socket.\n", intent.username);
				}
				else
				{
					userdataLock.lock();
					try{
						connectedUsers.remove(intent.username);
						userThreads.remove(intent.username);
					} finally{
						userdataLock.unlock();
					}
					System.out.println(intent.username + " has disconnected");
				}
			}
		}
	}	
}
