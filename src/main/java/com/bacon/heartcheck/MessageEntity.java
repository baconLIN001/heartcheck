package com.bacon.heartcheck;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by bacon on 2017/3/14.
 */
public class MessageEntity implements Serializable{

    private static final long serialVersionUID = -5834688224355882441L;

    private String MessageType = "Heartbeat";
    private String ClientName = "Localhost";
    private String ClientInfo = "I am alive!";

    /*public MessageEntity(String messageType, String clientName, String clientInfo){
        this.MessageType = messageType;
        this.ClientInfo = clientInfo;
        this.ClientName = clientName;
    }*/
    @Override
    public String toString() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\tHeartcheck Message: \n" +
                ClientName + " : " + "Tpye : " + MessageType + "; Info : " + ClientInfo;
    }
}
