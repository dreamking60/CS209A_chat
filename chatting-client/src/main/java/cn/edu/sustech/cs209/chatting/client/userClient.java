package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.stream.Collectors;

public class userClient implements Runnable {
    private String username;
    private Socket client;
    private Controller controller;
    private String[] users;

    public userClient(Socket client, String username, Controller controller) {
        this.client = client;
        this.username = username;
        this.controller = controller;
    }

    @Override
    public void run() {
        try{
            InputStream in = client.getInputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) != -1) {
                String msg = new String(buf, 0, len);
                System.out.println(msg);
                getMessage(msg);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getMessage(String msg) {
        try {
            String Head = msg.split(":")[0];
            if(Head.equals("MSG")) {
                String sendBy =  msg.split(":")[1];
                String msgBody = msg.substring(4+sendBy.length()+1);
                Platform.runLater(() -> controller.updateMsg(sendBy, msgBody));

            } else if(Head.equals("USERS")) {
                String userList = msg.substring(6);
                users = userList.split(",");
                Platform.runLater(() -> controller.updateUsers(users));

            } else if(Head.equals("GRP")) {
                String sendBy = msg.split(":")[1];
                String sendToGroup = msg.split(":")[2];
                String[] participant = msg.split(":")[3].split(",");
                String msgBody = msg.substring(4+sendBy.length()+1+sendToGroup.length()+1+msg.split(":")[3].length()+1);
                Platform.runLater(() -> controller.updateMsg(sendBy, msgBody, sendToGroup, participant));
            }

            return true;
        } catch (Exception e) {
            System.out.println("Get Message Error.");
            return false;
        }
    }

}
