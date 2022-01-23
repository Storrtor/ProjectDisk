package fx.client;

import common.Constants;
import common.ProtoFileSender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class Controller implements Initializable {

    @FXML
    Button signIn;

    @FXML
    Button signUp;

    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> clientFilesList;

    @FXML
    ListView<String> serverFilesList;

    // Отправка файла на сервер
    public void sendFile(String tfFileName) throws IOException {
        try {
            ProtoFileSender.sendFile(Paths.get("client_storage/" + tfFileName), Network.getInstance().getCurrentChannel(), future -> {
                if (!future.isSuccess()) {
                    JOptionPane.showConfirmDialog(null, "Файл не отправлен на сервер");
                }
                if (future.isSuccess()) {
                    JOptionPane.showConfirmDialog(null, "Файл успешно отправлен");
                }
            });
        } catch (NoSuchFileException e) {
            JOptionPane.showConfirmDialog(null, "Файл не найден");
        }
        refreshLocalFilesList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        try {
            networkStarter.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void pressOnSendButton(ActionEvent actionEvent) throws IOException {
        if (!tfFileName.getText().isEmpty()) {
            sendFile(ClientAuthHandler.getNick() + "/" + tfFileName.getText());
            tfFileName.clear();
        }
    }

    public void pressOnDownloud(ActionEvent actionEvent) {
        if (!tfFileName.getText().isEmpty()) {
            String filename = tfFileName.getText();
            sendFileRequest(filename, Network.getInstance().getCurrentChannel(), future -> {
                if (!future.isSuccess()) {
                    JOptionPane.showConfirmDialog(null, "Файл не загружен на сервер");
                }
                if (future.isSuccess()) {
                    JOptionPane.showConfirmDialog(null, "Файл успешно загружен");
                }
            });
            tfFileName.clear();
        }
        refreshLocalFilesList();
    }

    private void sendFileRequest(String filename, Channel outChannel, ChannelFutureListener futureListener) {
        byte[] filenameBytes = ("/request " + filename).getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length);
        buf.writeByte(Constants.CMD_SIGNAL_BYTE);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        ChannelFuture future = outChannel.writeAndFlush(buf);
        if (futureListener != null) {
            future.addListener(futureListener);
        }
    }

    public void pressOnDelete(ActionEvent actionEvent) {
        if (!tfFileName.getText().isEmpty()) {
            String filename = tfFileName.getText();
            sendDeleteRequest(filename, Network.getInstance().getCurrentChannel(), future -> {
                if (!future.isSuccess()) {
                    JOptionPane.showConfirmDialog(null, "Файл не удален на сервере");
                }
                if (future.isSuccess()) {
                    JOptionPane.showConfirmDialog(null, "Файл успешно удален");
                }
            });
            tfFileName.clear();
            refreshLocalFilesList();
        }
    }

    private void sendDeleteRequest(String filename, Channel outChannel, ChannelFutureListener futureListener) {
        byte[] filenameBytes = ("/delete " + filename).getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length);
        buf.writeByte(Constants.CMD_SIGNAL_BYTE);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        ChannelFuture future = outChannel.writeAndFlush(buf);
        if (futureListener != null) {
            future.addListener(futureListener);
        }
        refreshLocalFilesList();
    }

    public void refreshLocalFilesList() {
        Platform.runLater(() -> {
            try {
                clientFilesList.getItems().clear();
                Files.list(Paths.get("client_storage/" + ClientAuthHandler.getNick()))
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> clientFilesList.getItems().add(o));
                serverFilesList.getItems().clear();
                Files.list(Paths.get("server_storage/" + ClientAuthHandler.getNick()))
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> serverFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public void pressOnRefresh(ActionEvent actionEvent) {
        refreshLocalFilesList();
        tfFileName.clear();
    }

    public void pressOnAuth(ActionEvent actionEvent) throws InterruptedException {
        ModalWindow.newWindowAuth("Авторизация");
        Thread.sleep(1000);
        if (ClientAuthHandler.isAuthOk()) {
            signUp.setVisible(false);
            signUp.setManaged(false);
            signIn.setVisible(false);
            signIn.setManaged(false);
            refreshLocalFilesList();
        }
        refreshLocalFilesList();
    }

    public void pressOnSignUn(ActionEvent actionEvent) {
        ModalWindow.newWindowSignIn("Регистрация");
    }


}

