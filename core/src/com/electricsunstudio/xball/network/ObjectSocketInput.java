package com.electricsunstudio.xball.network;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class ObjectSocketInput extends Thread
{
    public boolean quit;
    Socket sock;
    ObjectInputStream objIn;
    //Gson gson;

    HashMap<Class,Handler> handlers = new HashMap<Class, Handler>();

    public ObjectSocketInput(Socket sock)
    {
        this.sock = sock;
        this.setPriority(Thread.MAX_PRIORITY);
        //gson = new Gson();
    }

    public void addHandler(Class cls, Handler handler)
    {
        handlers.put(cls, handler);
    }
    
    public void handleObject(Object o)
    {
        if(handlers.containsKey(o.getClass()))
            handlers.get(o.getClass()).onReceived(o);
        else
            System.out.printf("ObjectSocketInput: object of type %s ignored.", o.getClass().getSimpleName());
    }
    
    @Override
    public void run()
    {
        try {
            objIn = new ObjectInputStream(sock.getInputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            quit = true;
        }

        while(!quit && !sock.isClosed() && sock.isConnected())
        {
            try {
                Object obj = objIn.readObject();
                handleObject(obj);
//                if(rawObj instanceof String)
//                {
//                    ObjectWrapper wrapper = gson.fromJson((String)rawObj, ObjectWrapper.class);
//                    try{
//                        Class cls = Class.forName(wrapper.clsName);
//                        Object obj = gson.fromJson(wrapper.str, cls);
//                        handleObject(obj);
//                    } catch(ClassNotFoundException ex){
//                        System.out.printf("invalid object %s sent", wrapper.clsName);
//                    }
//                }
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
                quit = true;
                break;
            } catch (ClassNotFoundException ex) {
                System.out.println("invalid class received");
            }
        }
        System.out.println("ObjectSocketInput has been closed");
        
        if(!sock.isClosed())
            try {
                System.out.println("ObjectSocketInput closing socket");
                sock.close();
        } catch (IOException ex) {
                System.out.println("ObjectSocketInput exception closing thread");
        }
    }
}
