package cn.edu.sustech.cs209.chatting.client;

import java.net.Socket;

public class userClient implements Runnable {
    private String username;
    private Socket client;

    public userClient(Socket client, String username) {
        this.client = client;
        this.username = username;
    }

    @Override
    public void run() {

    }
}
