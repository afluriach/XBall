package com.electricsunstudio.xball.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.electricsunstudio.xball.network.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;

public class ServerLauncher {
	public static final int serverPort = 49000;
	static final int maxPacketSize = 65536;
	
	static HashMap<String,Connection> connectedUsers = new HashMap<String, Connection>();
	//the socket that each user is using for their connection
	//may be better to map it to the server thread using to handle each user since
	//the thread owns the socket and will handle the IO
	static HashMap<String, Thread> userThreads = new HashMap<String, Thread>();
	//keyed by the user name that created the match
	static HashMap<String, Match> pendingMatches = new HashMap<String, Match>();
	
	static Lock userdataLock = new ReentrantLock(true);
	
	static Gson gson;
	
	public static void main (String[] arg) {
		gson = new Gson();
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
						ObjectWrapper wrapper = gson.fromJson((String)obj, ObjectWrapper.class);
						
						try{
							Class cls = Class.forName(wrapper.clsName);
							if(!ClientIntent.class.isAssignableFrom(cls)){
								System.out.printf("non intent object of type %s sent to server.\n", wrapper.clsName);
							}
							ClientIntent intent = (ClientIntent) gson.fromJson(wrapper.str, cls);
							handleIntent(intent, conn);
						} catch(ClassNotFoundException ex){
							System.out.printf("invalid object %s sent", wrapper.clsName);
						}
						
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
			if(intent instanceof ConnectIntent)
			{
				ConnectIntent connect = (ConnectIntent) intent;
				if(user != null)
					System.out.printf("warning: multiple logins for %s", connect.username);
				
				user = connect.username;
				userdataLock.lock();
				try {
					connectedUsers.put(user, conn);
					userThreads.put(user, this);
				} finally {
					userdataLock.unlock();
				}
				System.out.println(user + " is now online, " + conn.addr + ":" + conn.port);	
			}
			else if(intent instanceof DisconnectIntent)
			{
				userdataLock.lock();
				try{
					connectedUsers.remove(user);
					userThreads.remove(user);
				} finally{
					userdataLock.unlock();
				}
				System.out.println(user + " has disconnected");
			}
		}
	}	
}
