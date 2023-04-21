package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Server {
    private ServerSocket server = null;
    private HashMap<String, Socket> clientList = new HashMap<>();
    // Server init
    public Server(int port) {
        try {
           server = new ServerSocket(port);
           System.out.println("Server init!");

           while(true) {
               Socket client = server.accept();
               System.out.println("Client "+client+" connected.");
               new Thread(new ClientHandler(client, this)).start();
           }

        } catch (IOException e) {
            System.out.println("Server Error.");
        }
    }

    public HashMap<String, Socket> getClientList() {
        return clientList;
    }

}
