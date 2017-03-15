package com.bacon.heartcheck;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bacon on 2017/3/15.
 */
public class Server {

    private int port;
    private volatile boolean runningStatus = false;
    private long receiveTimeInterval;
    private long checkMsgInterval;

    public interface HandleHeartCheckMsg{
        Object handle(Object receiver, Server server);
    }

    public static final class DefaultHandleAction implements HandleHeartCheckMsg{

        @Override
        public Object handle(Object receiver, Server server) {
            System.out.println("Server Handle: " + receiver);
            return receiver;
        }
    }

    public Server(int port){
        this.port = port;
        this.checkMsgInterval = ServerConfig.checkMsgInterval;
        this.receiveTimeInterval = ServerConfig.receiveTimeInterval;
    }

    private ConcurrentHashMap<Class, HandleHeartCheckMsg>handleActionMapping=new ConcurrentHashMap<Class, HandleHeartCheckMsg>();

    private Thread heartcheckWatcher;

    public void run(){
        if (runningStatus)
            return;
        else {
            runningStatus = true;
            heartcheckWatcher = new Thread(new HeartcheckWatcher());
            heartcheckWatcher.start();
        }
    }

    @SuppressWarnings("deprecation")
    public void stop(){
        if (runningStatus)runningStatus = false;
        if (heartcheckWatcher!=null)heartcheckWatcher.stop();
    }

    public void addHandleActionMap(Class<Object> objectClass, HandleHeartCheckMsg handleAction){
        handleActionMapping.put(objectClass,handleAction);
    }

    class HeartcheckWatcher implements Runnable{

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port,5);
                while (runningStatus){
                    Socket socket = serverSocket.accept();
                    new Thread(new SocketAction(socket)).start();
                }
            }catch (IOException e){
                e.printStackTrace();
                Server.this.stop();
            }
        }
    }

    class SocketAction implements Runnable{
        Socket socket;
        boolean running = true;
        long lastReceiveTime = System.currentTimeMillis();

        public SocketAction(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            while (running && runningStatus){
                if (System.currentTimeMillis() - lastReceiveTime > receiveTimeInterval){
                    overThis();
                }else {
                    try {
                        InputStream inputStream = socket.getInputStream();
                        if (inputStream.available() > 0){
                            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                            Object msg = objectInputStream.readObject();
                            lastReceiveTime = System.currentTimeMillis();
                            System.out.println("Server Receive: \t" + msg);
                            HandleHeartCheckMsg handleAction = handleActionMapping.get(msg.getClass());
                            handleAction = handleAction==null? new DefaultHandleAction():handleAction;
                            Object out = handleAction.handle(msg,Server.this);
                            if (out!=null){
                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                                objectOutputStream.writeObject(out);
                                objectOutputStream.flush();
                            }
                        }else {
                            Thread.sleep(checkMsgInterval);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        overThis();
                    }
                }
            }
        }

        private void overThis(){
            if (running)
                running = false;
            if (socket!=null){
                try {
                    socket.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            System.out.println("Close: " + socket.getRemoteSocketAddress());
        }
    }
}
