package com.bacon.heartcheck;


import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bacon on 2017/3/14.
 */
public class ClientHeartCheck {

    private String serverIp;
    private int port;
    private long checkAliveInterval;
    private long aliveInterval;
    private long checkFeedbackInterval;

    private Socket socket;
    private boolean runningStatus = false;

    private long lastSendTime;

    //mapping the received feedback message tp the action object the handle the message.
    private ConcurrentHashMap<Class, HandleFeedbackAction> msgActionMapping = new ConcurrentHashMap<Class, HandleFeedbackAction>();

    public static interface HandleFeedbackAction{
        void handle(Object reader, ClientHeartCheck clientHeartCheck);
    }

    public static final class DefaultFeedbackAction implements HandleFeedbackAction{

        @Override
        public void handle(Object reader, ClientHeartCheck clientHeartCheck) {
            System.out.println("Client Handle: \t" + reader.toString());
        }
    }

    public ClientHeartCheck(String serverIp, int port){
        this.serverIp = serverIp;
        this.port = port;
        this.checkAliveInterval = ClientConfig.checkAliveInterval;
        this.aliveInterval = ClientConfig.aliveInterval;
        this.checkFeedbackInterval = ClientConfig.checkFeedbackInterval;
    }



    public void run() throws UnknownHostException, IOException{
        if (runningStatus)
            return;
        socket = new Socket(serverIp,port);
        System.out.println("Client Port: " + socket.getLocalPort());
        lastSendTime = System.currentTimeMillis();
        runningStatus = true;
        new Thread(new KeepClientAlive()).start();
        new Thread(new ReceiveFeedback()).start();
    }

    public void stop(){
        if (runningStatus)
            runningStatus = false;
    }

    public void addMsgActionMap(Class<Object> objectClass, HandleFeedbackAction handleAction){
        msgActionMapping.put(objectClass,handleAction);
    }

    public void sendHeartbeat(Object sender) throws IOException
    {
        //MessageEntity messageEntity = new MessageEntity();
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(sender);
        System.out.println("Client Send: \t" + sender);
        oos.flush();
    }

    class KeepClientAlive implements Runnable{

        @Override
        public void run() {
            while (runningStatus){
                if (System.currentTimeMillis() - lastSendTime > aliveInterval){
                    try {
                        ClientHeartCheck.this.sendHeartbeat(new MessageEntity());
                    }catch (IOException e){
                        e.printStackTrace();
                        ClientHeartCheck.this.stop();
                    }
                    lastSendTime = System.currentTimeMillis();
                }else {
                    try {
                        Thread.sleep(checkAliveInterval);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                        ClientHeartCheck.this.stop();
                    }
                }
            }
        }
    }

    class ReceiveFeedback implements Runnable{

        @Override
        public void run() {
            while (runningStatus){
                try {
                    InputStream feedbackStream = socket.getInputStream();
                    if (feedbackStream.available()>0){
                        ObjectInputStream feedbackMsgStream = new ObjectInputStream(feedbackStream);
                        Object reader = feedbackMsgStream.readObject();
                        System.out.println("Client Receive: \t" + reader);

                        HandleFeedbackAction msgAction = msgActionMapping.get(reader.getClass());
                        msgAction = msgAction == null ? new DefaultFeedbackAction() : msgAction;
                        msgAction.handle(reader,ClientHeartCheck.this);
                    }else {
                        Thread.sleep(checkFeedbackInterval);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    ClientHeartCheck.this.stop();
                }
            }
        }
    }
}
