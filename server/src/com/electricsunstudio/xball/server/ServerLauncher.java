package com.electricsunstudio.xball.server;

import java.util.Timer;
import com.electricsunstudio.xball.Game;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.electricsunstudio.xball.network.*;
import com.electricsunstudio.xball.ControlState;
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
import java.util.TimerTask;

public class ServerLauncher {
    public static final int serverPort = 49000;
    static final int maxPacketSize = 65536;
    static final float engineDelay = 0.2f;
    
    static HashMap<String,Connection> connectedUsers = new HashMap<String, Connection>();
    //the socket that each user is using for their connection
    //may be better to map it to the server thread using to handle each user since
    //the thread owns the socket and will handle the IO
    static HashMap<String, ServerThread> userThreads = new HashMap<String, ServerThread>();
    static ArrayList<ServerThread> serverThreads = new ArrayList();
    
    static Lock userdataLock = new ReentrantLock(true);
    
    static Gson gson;
    
    //engine running the current match
    static Timer engineTimer;
    static Game game;
    static float updateTimeAcc;
    
    static class EngineTick extends TimerTask
    {
        long lastTick = Long.MIN_VALUE;
        long getDelta()
        {
            if(lastTick != Long.MIN_VALUE)
            {
                long crnt = System.currentTimeMillis();
                long delta = crnt - lastTick;
                lastTick = crnt;
                return delta;
            }
            else
            {
                lastTick = System.currentTimeMillis();
                return 0;
            }
        }
        
        @Override
        public void run() {
            long delta = getDelta();
            updateTimeAcc += delta/1000f;
            
            while(updateTimeAcc >= engineDelay)
            {
//                System.out.printf("running update tick for frame %d\n", game.engine.crntFrame);
                game.updateTick();
                updateTimeAcc -= Game.SECONDS_PER_FRAME;
            }
        }
    }
    
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
                    System.out.println("no team size provided");
                }
                if(tokens.length < 5)
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
                
                int teamSize=0;
                try{
                    teamSize = Integer.parseInt(tokens[3]);
                    if(teamSize <= 0 || teamSize > 3)
                        throw new NumberFormatException();
                } catch(NumberFormatException e)
                {
                    System.out.println("invalid team size");
                    continue;
                }
                
                startMatch(mapName, level, Arrays.copyOfRange(tokens, 4, tokens.length), teamSize);
            }
            else
            {
                System.out.println("unknown command " + line);
            }
        }
    }

    //TODO lookup players in level map, ensure the names are valid
    //warn if player has not been assigned
    static void startMatch(String levelName, Class level, String[] tokens, int teamSize)
    {
        HashMap<String,StartMatch> messages = new HashMap<String, StartMatch>();
        long seed = System.currentTimeMillis();
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
                    messages.put(username, new StartMatch(levelName, playerName, seed, teamSize));
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
        startMatchGame(levelName, seed, teamSize);
    }
    
    static void startMatchGame(String levelName, long seed, int teamSize)
    {
        game = new Game(seed);
        Game.level = Game.getLevelFromSimpleName(levelName);
        Game.teamSize = teamSize;
        game.createServer();

        engineTimer = new Timer(true);
        engineTimer.schedule(new EngineTick(), 0, 10);
    }
    
    //send to all other threads
    static void notifyControls(ControlState cs, ServerThread from)
    {
        for(ServerThread t : userThreads.values())
        {
            if(t == from) continue;
            t.objOut.send(cs);
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
                    clientConnection.setTcpNoDelay(true);
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
            
            //add listener for control state. populate the player field for this
            //player and send to all other clients
            objIn.addHandler(ControlState.class, new Handler(){
            public void onReceived(Object t) {
                ControlState cs = (ControlState)t;
                cs.player = player;
                notifyControls(cs, ServerThread.this);
                }
            });
            
            //add listener for ping. send the object right back to the same client
            objIn.addHandler(PingIntent.class, new Handler(){
                @Override
                public void onReceived(Object t) {
                    objOut.send(t);
                }
            });
        }
        
        @Override
        public void run()
        {
            objIn.start();
            objOut.start();
            
            while(true)
            {
                if(sock.isClosed() || sock.isInputShutdown() || sock.isOutputShutdown())
                {
                    if(user != null)
                        System.out.printf("user %s, socket closed without disconnect\n", user);
                    userThreads.remove(user);
                    serverThreads.remove(this);
                    connectedUsers.remove(user);
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
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
