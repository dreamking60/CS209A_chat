package cn.edu.sustech.cs209.chatting.client;

import java.net.Socket;

public class userClient implements Runnable {
    private String userId;
    private Socket client;

    public userClient(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {

    }
}
