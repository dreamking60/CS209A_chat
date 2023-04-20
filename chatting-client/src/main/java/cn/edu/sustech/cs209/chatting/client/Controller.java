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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    private Socket client;
    private String[] users;
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

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */

            username = input.get();
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
                    }

                    dialog.setContentText("Username already exists. Please choose another username:");
                    Optional<String> reInput = dialog.showAndWait();
                    if (reInput.isPresent() && !reInput.get().isEmpty()) {
                        username = reInput.get();
                    } else {
                        System.out.println("Invalid username " + reInput + ", exiting");
                        Platform.exit();
                    }

                } while(flag);

                client.getOutputStream().write(username.getBytes());
                new Thread(new userClient(client, username)).start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

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
        getUsers();
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
        chatList.getSelectionModel().select(chooseUser);
        chat = chats.get(chatItems.indexOf(chooseUser));
        chatContentItem = chat.getMessages();
        chatContentList.setItems(chatContentItem);

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
    public void createGroupChat() {
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

    public void addChatRecord(String chatRecord) {
        chatItems.add(chatRecord);
    }



    public void getUsers() {
        try {
            client.getOutputStream().write("GETUSERS:".getBytes());
            InputStream in = client.getInputStream();
            byte[] buf = new byte[1024];
            int len = in.read(buf);
            String userList = new String(buf, 0, len);
            if(userList.startsWith("USERS:")){
                userList = userList.substring(6);
                users = userList.split(",");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
