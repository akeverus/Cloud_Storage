package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    public ListView<String> listViewClient;
    public ListView<String> listViewServer;
    public TextField textFieldClient;
    public TextField textFieldServer;

    private DataInputStream is;
    private DataOutputStream os;

    private File currentDir;
    private File serverDir;

    private byte[] buf;

    public void sendMessageToServer(ActionEvent actionEvent) throws IOException {
        String fileName = textFieldClient.getText();
        File currentFile = currentDir.toPath().resolve(fileName).toFile();
        os.writeUTF("#SEND#FILE#");
        os.writeUTF(fileName);
        os.writeLong(currentFile.length());
        try (FileInputStream is = new FileInputStream(currentFile)) {
            while (true) {
                int read = is.read(buf);
                if (read == - 1) {
                    break;
                }
                os.write(buf, 0, read);
            }
        }
        os.flush();
        fillServerDirFiles();
        textFieldClient.clear();
    }

    public void sendMessageToClient(ActionEvent actionEvent) throws IOException {
        String fileName = textFieldServer.getText();
        File currentFile = serverDir.toPath().resolve(fileName).toFile();
        os.writeUTF("#RECEIVE#FILE#");
        os.writeUTF(fileName);
        os.writeLong(currentFile.length());
        try (FileInputStream is = new FileInputStream(currentFile)) {
            while (true) {
                int read = is.read(buf);
                if (read == - 1) {
                    break;
                }
                os.write(buf, 0, read);
            }
        }
        os.flush();
        fillCurrentDirFiles();
        textFieldServer.clear();
    }

    private void read() {
        try {
            while (true) {
                String message = is.readUTF();
                Platform.runLater(() -> textFieldClient.setText(message));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // reconnect to server
        }
    }

    private void fillCurrentDirFiles() {
        listViewClient.getItems().clear();
        listViewClient.getItems().add("..");
        listViewClient.getItems().addAll(currentDir.list());
    }

    private void fillServerDirFiles() {
        listViewServer.getItems().clear();
        listViewServer.getItems().add("..");
        listViewServer.getItems().addAll(serverDir.list());
    }

    private void initClickListener() {
        listViewClient.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileName = listViewClient.getSelectionModel().getSelectedItem();
                System.out.println("Выбран файл: " + fileName);
                Path path = currentDir.toPath().resolve(fileName);
                if (Files.isDirectory(path)) {
                    currentDir = path.toFile();
                    fillCurrentDirFiles();
                    textFieldClient.clear();
                } else {
                    textFieldClient.setText(fileName);
                }
            }
        });
        listViewServer.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileName = listViewServer.getSelectionModel().getSelectedItem();
                System.out.println("Выбран файл: " + fileName);
                Path path = serverDir.toPath().resolve(fileName);
                if (Files.isDirectory(path)) {
                    serverDir = path.toFile();
                    fillServerDirFiles();
                    textFieldServer.clear();
                } else {
                    textFieldServer.setText(fileName);
                }
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            buf = new byte[256];
            currentDir = new File(System.getProperty("user.home"));
            serverDir = new File("serverDir");
            fillCurrentDirFiles();
            fillServerDirFiles();
            initClickListener();
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
