package com.electricsunstudio.xball.server;

import com.electricsunstudio.xball.Game;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.electricsunstudio.xball.network.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Scanner;

public class ServerLauncher {
	public static final int serverPort = 49000;
	static final int maxPacketSize = 65536;
	
	static HashMap<String,Connection> connectedUsers = new HashMap<String, Connection>();
	//the socket that each user is using for their connection
	//may be better to map it to the server thread using to handle each user since
	//the thread owns the socket and will handle the IO
	static HashMap<String, ServerThread> userThreads = new HashMap<String, ServerThread>();
	static ArrayList<ServerThread> serverThreads = new ArrayList();
	
	static Lock userdataLock = new ReentrantLock(true);
	
	static Gson gson;
	
	public static void main (String[] arg) {
		gson = new Gson();
		MainServerThread serverThread = new MainServerThread(serverPort);
		serverThread.start();
		
		Scanner fin = new Scanner(System.in);
		
		while(true)
		{
			String line = fin.nextLine();
			
			if(line.equalsIgnoreCase("exit"))
			{
				serverThread.quit = true;
				try {
					serverThread.sock.close();
				} catch (IOException ex) {
				}
				
				for(ServerThread thread : serverThreads)
				{
					thread.close();
				}
				break;
			}
			else if(line.startsWith("start match "))
			{
				String[] tokens = line.split(" ");
				if(tokens.length < 3)
				{
					System.out.println("no map name provided");
					continue;
				}
				if(tokens.length < 4)
				{
					System.out.println("no users to join provided");
					continue;
				}
				
				//start match level {user=player_name}
				String mapName = tokens[2];
				
				Class level = Game.getLevelFromSimpleName(mapName);
				
				if(level == null)
				{
					System.out.println("invalid level");
					continue;
				}
				startMatch(mapName, level, Arrays.copyOfRange(tokens, 3, tokens.length));
			}
			else
			{
				System.out.println("unknown command " + line);
			}
		}
	}

	//TODO lookup players in level map, ensure the names are valid
	//warn if player has not been assigned
	static void startMatch(String levelName, Class level, String[] tokens)
	{
		for(String s : tokens)
		{
			System.out.println(s);
		}
		
		HashMap<String,StartMatch> messages = new HashMap<String, StartMatch>();
		try
		{
			userdataLock.lock();
			//check that each user is logged in. if so, send start match message
			for(int i=0; i < tokens.length; ++i)
			{
				String[] t = tokens[i].split("=");
				if(t.length != 2)
				{
					System.out.println("malformed token " + tokens[i]);
					return;
				}
				String username = t[0];
				String playerName = t[1];

				if(!userThreads.containsKey(username))
				{
					System.out.println(username + " is not online");
					return;
				}
				else
				{
					messages.put(username, new StartMatch(levelName, playerName));
				}
			}
		}
		finally
		{
			userdataLock.unlock();
		}
		
		//send the messages and start match
		for(Entry<String,StartMatch> e : messages.entrySet())
		{
			userThreads.get(e.getKey()).startMatch(e.getValue());
			System.out.println("sent start message to " + e.getKey());
		}
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
		public ServerSocket sock;
		boolean quit = false;
		public MainServerThread(int port)
		{
			try {
				sock = new ServerSocket(port);
			} catch (BindException ex) {
				System.out.println("port in use");
				quit = true;
			} catch (IOException ex) {
				ex.printStackTrace();
				quit = true;
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
					ServerThread t = new ServerThread(clientConnection);
					t.start();
					serverThreads.add(t);
				} catch (IOException ex) {
					break;
				}
			}
		}
	}
	
	//per client socket opened. listen for intents by client and communicate
	//result
	static class ServerThread extends Thread
	{
		public Socket sock;
		Connection conn;
		
		String user;
		String player;
		
		ObjectSocketInput objIn;
		ObjectSocketOutput objOut;
		
		boolean quit;
		
		public ServerThread(Socket sock)
		{
			this.sock = sock;
			conn = new Connection(sock.getInetAddress(), sock.getPort());
			
			try {
				objIn = new ObjectSocketInput(sock);
				objOut = new ObjectSocketOutput(sock);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			objIn.addHandler(ConnectIntent.class, new Handler(){
				@Override
				public void onReceived(Object o) {
					ConnectIntent connect = (ConnectIntent) o;
					if(user != null)
					{
						System.out.printf("%s has attempted another login on the same socket.\n", connect.username);
						objOut.send(new LoginResponse("Already logged in", false));
						return;
					}
					if(connect.username.contains(" "))
					{
						objOut.send(new LoginResponse("Spaces not allowed in username", false));
						System.out.printf("login attempt with invalid username %s.\n", connect.username);
                        return;
					}

					boolean uniqueUser = false;
					user = connect.username;
					userdataLock.lock();
					try {
						if(!connectedUsers.containsKey(connect.username))
						{
						    connectedUsers.put(user, conn);
						    userThreads.put(user, ServerThread.this);
							uniqueUser = true;
						}
					} finally {
						userdataLock.unlock();
					}
					if(uniqueUser)
					{
						System.out.println(user + " is now online, " + conn.addr + ":" + conn.port);
						objOut.send(new LoginResponse("Success", true));
					}
					else
					{
						System.out.println("duplicate username " + user + conn.addr + ":" + conn.port);
						objOut.send(new LoginResponse("Username is already in use", false));
					}
				}
			});
			
			objIn.addHandler(DisconnectIntent.class, new Handler<DisconnectIntent>(){

				@Override
				public <DisconnectIntent> void onReceived(DisconnectIntent t) {
					userdataLock.lock();
					try{
						connectedUsers.remove(user);
						userThreads.remove(user);
					} finally{
						userdataLock.unlock();
					}
					System.out.println(user + " has disconnected");
					user = null;
				}
			});
		}
		
		public void startMatch(StartMatch m)
		{
			player = m.player;
			objOut.send(m);
		}
		
		@Override
		public void run()
		{
			objIn.start();
			objOut.start();
		}
		
		public void close()
		{
			objOut.send(new ServerShutdown());
			objOut.clear();
			try {
				sock.close();
			} catch (IOException ex) {
			}
			quit = true;
		}
	}
}
