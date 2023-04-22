package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    public TextArea inputArea;
    public Label currentUsername;
    private Socket client;
    String[] users;
    Chat chat;
    @FXML
    ListView<String> chatList;
    @FXML
    ListView<Message> chatContentList;
    private ObservableList<Chat> chats;
    private ObservableList<String> chatItems;
    private ObservableList<Message> chatContentItem;
    private String username;
    private String selectedUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        getUsername();
        getAndCheckUserList();
        currentUsername.setText(username);
        openListener();
        setupChatInterface();
    }

    public void getUsername() {
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if(input.isPresent() && !input.get().isEmpty()) {
            username = input.get();
        } else {
            System.out.println("Invalid username, exiting");
            Platform.exit();
        }

    }

    public void getAndCheckUserList() {
        try {
            client = new Socket("127.0.0.1", 16000);

            // get user list
            InputStream in = client.getInputStream();
            byte[] buf = new byte[1024];
            int len = in.read(buf);
            String userList = new String(buf, 0, len);
            if(userList.startsWith("USERS:")){
                userList = userList.substring(6);
                users = userList.split(",");
            }
            System.out.println(Arrays.toString(users));

            // check if username is in the list
            boolean flag = false;
            do {
                if(users != null) {
                    flag = Arrays.stream(users).anyMatch(user -> user.equals(username));
                }
                if(!flag) {
                    break;
                } else {
                    Dialog<String> dialog = new TextInputDialog();
                    dialog.setTitle("Login");
                    dialog.setHeaderText(null);
                    dialog.setContentText("Username already exists. Please choose another username:");
                    Optional<String> reInput = dialog.showAndWait();
                    if (reInput.isPresent() && !reInput.get().isEmpty()) {
                        username = reInput.get();
                    } else {
                        System.out.println("Invalid username " + reInput + ", exiting");
                        Platform.exit();
                        break;
                    }
                }

            } while(flag);

            client.getOutputStream().write(username.getBytes());
        } catch (IOException e) {
            System.out.println("No username.");
        }

    }
    public void openListener() {
        // msg Listener
        Thread msgListener = new Thread(new userClient(client, username, this));
        msgListener.setDaemon(true);
        msgListener.start();

        // chatList Listener
        chatListListener();
    }

    public void setupChatInterface() {
        chats = FXCollections.observableArrayList();
        chatItems = FXCollections.observableArrayList();
        chatList.setItems(chatItems);
        chatContentList.setCellFactory(new MessageCellFactory());
    }

    @FXML
    public void createPrivateChat() throws IOException, InterruptedException {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        // set title
        stage.setTitle("Create Private Chat");
        // set size
        stage.setWidth(300);
        stage.setHeight(100);

        ComboBox<String> userSel = new ComboBox<>();
        userSel.setPromptText("Select a user");

        // FIXME: get the user list from server, the current user's name should be filtered out
        userSel.getItems().addAll(Arrays.stream(users).filter(u -> !u.equals(username)).toArray(String[]::new));

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        String chooseUser = userSel.getValue();
        if (chooseUser == null) {
            return;
        }
        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        if (!chatItems.contains(chooseUser)) {
            chatItems.add(chooseUser);
            chats.add(new Chat(username, chooseUser));
        }
        selectedUser = chooseUser;
        chatList.getSelectionModel().select(selectedUser);

    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() throws IOException, InterruptedException {
        Stage stage = new Stage();
        // set title
        stage.setTitle("Create Group Chat");
        // set size
        stage.setWidth(300);
        stage.setHeight(350);

        Label userLabel = new Label("Select users to add to the group:");
        VBox userBox = new VBox(10, userLabel);

        List<CheckBox> checkBoxList = new ArrayList<>();

        for (String user : users) {
            if (!user.equals(username)) {
                CheckBox checkBox = new CheckBox(user);
                checkBoxList.add(checkBox);
                userBox.getChildren().add(checkBox);
            }
        }

        TextArea groupNameField = new TextArea();
        groupNameField.setPromptText("Enter group name");
        groupNameField.setWrapText(true);
        groupNameField.setPrefRowCount(2);
        VBox nameBox = new VBox(10, new Label("Group name:"), groupNameField);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            List<String> selectedUsers = new ArrayList<>();
            for (CheckBox checkBox : checkBoxList) {
                if (checkBox.isSelected()) {
                    selectedUsers.add(checkBox.getText());
                }
            }
            selectedUsers.add(username);
            String groupName = groupNameField.getText().trim();
            if (groupName.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Group name cannot be empty");
                alert.showAndWait();
                return;
            }
            if(selectedUsers.size() <= 2) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Group must have at least 3 users");
                alert.showAndWait();
                return;
            }
            stage.close();
            newGroupChat(groupName, selectedUsers);
        });

        HBox btnBox = new HBox(10, okBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox box = new VBox(10, userBox, nameBox, btnBox);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        stage.setScene(new Scene(box));
        stage.showAndWait();


    }

    // create a new group chat
    private void newGroupChat(String groupName, List<String> users) {
        // Check if the group chat already exist
        for (Chat chat : chats) {
            if (chat.getChatName().equals(groupName)) {
                // If the group chat already exist, select the existing chat in the chat list
                selectedUser = groupName;
                chatList.getSelectionModel().select(selectedUser);
                return;
            }
        }

        // If the group chat does not exist, create a new chat item in the left panel, the title should be the group name
        chatItems.add(groupName);
        chats.add(new Chat(username, groupName, users));

        selectedUser = groupName;
        chatList.getSelectionModel().select(selectedUser);
    }


    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        if (inputArea.getText().isBlank() || chat == null) {
            return;
        }
        String msg = inputArea.getText();
        Long timestamp = System.currentTimeMillis();
        if(chat.isGroupChat) {
            SendGroupMessage(msg);
        } else {
            Send(msg);
        }
        chats.get(chatItems.indexOf(selectedUser)).addMessage(timestamp, msg);
        inputArea.clear();
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    ObservableList<Message> chatContentItem = getListView().getItems(); // 获取当前的chatContentItem
                    if (!chatContentItem.contains(msg)) { // 检查新的消息是否在chatContentItem中
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    // update chat content
    public void chatListListener() {
        chatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedUser = newValue;
                chat = chats.get(chatItems.indexOf(newValue));
                chatContentItem = chat.getMessages();
                chatContentList.setItems(FXCollections.observableArrayList());
                chatContentList.getItems().clear();
                chatContentList.setItems(chatContentItem);
                chatContentList.scrollTo(chatContentItem.size() - 1);
            }
        });
    }

    // send msg to a user
    public void Send(String msg) {
        try {
            String sendTo = chat.getChatName();
            String msgContent = "MSG:" + sendTo + ":" + msg;
            client.getOutputStream().write(msgContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // send msg to a group
    private void SendGroupMessage(String msg) {
        try {
            String sendToGroup = chat.getChatName();
            String sendToParticipant = String.join(",", chat.getParticipant());
            String sendBy = username;
            String msgContent = "GRP:" + sendBy + ":" + sendToGroup + ":" + sendToParticipant + ":" + msg;

            client.getOutputStream().write(msgContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // update user list
    public void updateUsers(String[] users) {
        this.users = users;
        for(String user : users) {
            if(!chatItems.contains(user) && !user.equals(username)) {
                chatItems.add(user);
                chats.add(new Chat(username, user));
            }
        }
    }

    // update one-to-one chat message
    public void updateMsg(String sendBy, String msgBody) {
        if(!chatItems.contains(sendBy)) {
            chatItems.add(sendBy);
            chats.add(new Chat(username, sendBy));
        }
        Long timestamp = System.currentTimeMillis();
        chats.get(chatItems.indexOf(sendBy)).getMessages(timestamp, msgBody);
    }

    // update group chat message
    public void updateMsg(String sendBy, String msgBody, String sendToGroup, String[] participant) {
        if(!chatItems.contains(sendToGroup)) {
            chatItems.add(sendToGroup);
            chats.add(new Chat(username, sendToGroup, Arrays.stream(participant).toList()));
        }
        Long timestamp = System.currentTimeMillis();
        chats.get(chatItems.indexOf(sendToGroup)).getMessages(timestamp, msgBody, sendBy);
    }

}
