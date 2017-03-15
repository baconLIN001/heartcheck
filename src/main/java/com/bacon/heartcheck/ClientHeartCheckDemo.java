package com.bacon.heartcheck;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by bacon on 2017/3/14.
 */
public class ClientHeartCheckDemo {
    public static void main(String[] args) throws UnknownHostException, IOException{
        String serverIp = ClientConfig.ServerIp;
        //String serverIp = "127.0.0.1";
        int clientPort = ClientConfig.ClientPort;
        int serverPort = ServerConfig.ServerPort;
        //int port = 32358;
        Server server = new Server(serverPort);
        ClientHeartCheck clientHeartCheck = new ClientHeartCheck(serverIp,clientPort);
        server.run();
        clientHeartCheck.run();
    }
}
