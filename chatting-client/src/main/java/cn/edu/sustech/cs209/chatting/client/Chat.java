package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class Chat {
    private String clientUser;
    private String chatName;
    private ObservableList<Message> messages;
    private ObservableList<String> participant;
    private List<String> members;

    public boolean isGroupChat = false;

    public Chat(String clientUser, String chatName) {
        this.clientUser = clientUser;
        this.chatName = chatName;
        messages = FXCollections.observableArrayList();
    }

    public Chat(String clientUser, String chatName, List<String> participant) {
        this.clientUser = clientUser;
        this.chatName = chatName;
        this.members = participant;
        this.participant = FXCollections.observableArrayList();
        this.participant.addAll(participant);
        messages = FXCollections.observableArrayList();
        isGroupChat = true;
    }


    public String getClientUser() {
        return clientUser;
    }

    public String getChatName() {
        return chatName;
    }

    public ObservableList<Message> getMessages() {
        return messages;
    }

    public ObservableList<String> getParticipant() {
        return participant;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public void addMessage(Long timestamp, String data) {
        messages.add(new Message(timestamp, clientUser, chatName, data));
    }

    public void getMessages(Long timestamp, String data) {
        messages.add(new Message(timestamp, chatName, clientUser, data));
    }

    public void getMessages(Long timestamp, String data, String sendBy) {
        messages.add(new Message(timestamp, sendBy, chatName, data));
    }
}
