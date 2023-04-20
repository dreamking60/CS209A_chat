package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable{
    private Socket client;
    private String clientId;
    private Server server;
    public ClientHandler(Socket client, Server server) {
        this.client = client;
        this.server = server;
    }
    @Override
    public void run() {
        try {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            // Send user list
            sendMessage("GETUSERS:");

            // Read client id
            byte[] id = new byte[20];
            int clientIdLen = in.read(id);
            clientId = new String(id, 0, clientIdLen);
            server.getClientList().put(clientId, client);

            // Read the message from client
            byte[] msg = new byte[1024];
            int msgLen;
            while((msgLen = in.read(msg)) != -1) {
                String msgStr = new String(msg, 0, msgLen);
                System.out.println("Client: " + msgStr);

                // send message to target client
                sendMessage(msgStr);
            }

        } catch (Exception e) {
            System.out.println("ClientHandler Error.");
        } finally {
            try {
                server.getClientList().remove(clientId);
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Send message to target client
    public synchronized boolean sendMessage(String msg) {
        try {
            String Head = msg.split(":")[0];
            if(Head.equals("MSG")) {
                String targetClientId =  msg.split(":")[1];
                String msgContent = msg.split(":")[2];

                Socket targetClient = server.getClientList().get(targetClientId);
                if (targetClient == null) {
                    System.out.println("Target Client Not Found.");
                    return false;
                }
                OutputStream out = targetClient.getOutputStream();
                out.write(msgContent.getBytes());
            } else if(Head.equals("GETUSERS")) {
                String usersStr = server.getClientList().keySet().stream().collect(Collectors.joining(","));
                String msgContent = "USERS:" + usersStr;
                System.out.println(msgContent);
                OutputStream out = client.getOutputStream();
                out.write(msgContent.getBytes());
            } else if(Head.equals("LOGOUT")) {
                server.getClientList().remove(clientId);
                client.close();
            }

            return true;
        } catch (Exception e) {
            System.out.println("Send Message Error.");
            return false;
        }
    }

    // return user list
    public void useList() {

    }

}
