package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Chat {
    private String participant1;
    private String participant2;
    private ObservableList<Message> messages;

    public Chat(String participant1, String participant2) {
        this.participant1 = participant1;
        this.participant2 = participant2;
        messages = FXCollections.observableArrayList();
    }

    public String getParticipant1() {
        return participant1;
    }

    public String getParticipant2() {
        return participant2;
    }

    public ObservableList<Message> getMessages() {
        return messages;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }
}
