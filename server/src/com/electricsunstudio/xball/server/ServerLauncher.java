package com.electricsunstudio.xball.server;

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
import java.util.ArrayList;
import java.util.Scanner;

public class ServerLauncher {
	public static final int serverPort = 49000;
	static final int maxPacketSize = 65536;
	
	static HashMap<String,Connection> connectedUsers = new HashMap<String, Connection>();
	//the socket that each user is using for their connection
	//may be better to map it to the server thread using to handle each user since
	//the thread owns the socket and will handle the IO
	static HashMap<String, Thread> userThreads = new HashMap<String, Thread>();
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
						System.out.printf("%s has attempted another login on the same socket.", connect.username);
						objOut.send(new LoginResponse("Already logged in", false));
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
