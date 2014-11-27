package com.electricsunstudio.xball.network;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObjectSocketOutput extends Thread
{
    public static final int capacity = 32;
    public static final long timeout = 100;
    
    public boolean quit;
    public Socket sock;
    BlockingQueue<Object> sendQueue;
    ObjectOutputStream objOut;
    //Gson gson;

    InetAddress addr;
    int port;

    public ObjectSocketOutput(InetAddress addr, int port) throws IOException
    {
        this.addr = addr;
        this.port = port;

        sendQueue = new ArrayBlockingQueue<Object>(capacity);

        sock = new Socket(addr, port);
        objOut = new ObjectOutputStream(sock.getOutputStream());
        this.setPriority(Thread.MAX_PRIORITY);
    }
    
    public ObjectSocketOutput(Socket sock) throws IOException
    {
        this.addr = sock.getInetAddress();
        this.port = sock.getPort();
        
        sendQueue = new ArrayBlockingQueue<Object>(capacity);
        
        this.sock = sock;
        objOut = new ObjectOutputStream(sock.getOutputStream());
        this.setPriority(Thread.MAX_PRIORITY);
    }

    public void send(Object obj)
    {
        try {
            sendQueue.put(obj);
        } catch (InterruptedException ex) {
            System.out.println("interrupted while waiting to add " + obj);
        }
    }
    
    //does not block if queue is full
    public boolean sendNoBlock(Object obj)
    {
        return sendQueue.offer(obj);
    }
    
    @Override
    public void run()
    {
        while(!quit && !sock.isClosed() && sock.isConnected())
        {
            Object obj = null;
            try {
                obj = sendQueue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                System.out.println("interrupted while waiting for next object to send");
            }
            
            if(obj != null)
            {
                try {
                    objOut.writeObject(obj);
                    //objOut.writeObject(gson.toJson(new ObjectWrapper(obj.getClass().getName(),gson.toJson(obj))));
                } catch (NotSerializableException ex){
                        ex.printStackTrace();
                } catch (IOException ex) {
                    System.out.println("output socket closed");
                    quit = true;
                }
            }
        }
        
        if(!sock.isClosed())
            try {
                sock.close();
        } catch (IOException ex) {
                System.out.println("excpetion closing socket output thread");
        }
    }

    public void clear() {
        Object obj;
        
        do
        {
            obj = sendQueue.poll();
            
            if(obj != null)
            {
                try {
                    //objOut.writeObject(gson.toJson(new ObjectWrapper(obj.getClass().getName(),gson.toJson(obj))));
                    objOut.writeObject(obj);
                } catch (NotSerializableException ex){
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }while(obj != null);
    }
}
