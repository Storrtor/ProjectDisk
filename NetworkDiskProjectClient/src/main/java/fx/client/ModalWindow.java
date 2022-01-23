package fx.client;

import common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;

public class ModalWindow {

    public static void newWindowAuth(String title) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        Pane pane = new TilePane();

        TextField logField = new TextField();
        TextField passField = new TextField();

        Button button = new Button("Войти");
        Label logLabel = new Label("Введите логин");
        Label passLabel = new Label("Введите пароль");
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String auth = logField.getText() + " " + passField.getText();
                byte[] authArray = auth.getBytes(StandardCharsets.UTF_8);
                ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(authArray.length);
                buf.writeByte(Constants.AUTH_NOT_OK);
                buf.writeInt(authArray.length);
                buf.writeBytes(authArray);
                System.out.println("Авторизация с клиента");
                Network.getInstance().getCurrentChannel().writeAndFlush(buf);
                logField.clear();
                passField.clear();
                window.close();
            }
        });

        pane.getChildren().addAll(logLabel, logField, passLabel, passField, button);

        Scene scene = new Scene(pane, 170, 150);
        window.setResizable(false);
        window.setScene(scene);
        window.setTitle(title);
        window.showAndWait();

    }

    public static void newWindowSignIn(String title) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        Pane pane = new TilePane();

        TextField logField = new TextField();
        TextField passField = new TextField();
        TextField nickField = new TextField();

        Button button = new Button("Зарегистрироваться");
        Label nickLabel = new Label("Введите имя");
        Label logLabel = new Label("Введите логин");
        Label passLabel = new Label("Введите пароль");
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!nickField.getText().isEmpty() && !logField.getText().isEmpty() && !passField.getText().isEmpty()) {
                    String signIn = nickField.getText() + " " + logField.getText() + " " + passField.getText();
                    byte[] signInArray = signIn.getBytes(StandardCharsets.UTF_8);
                    ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(signInArray.length);
                    buf.writeByte(Constants.COMMAND_SIGN_IN);
                    buf.writeInt(signInArray.length);
                    buf.writeBytes(signInArray);
                    System.out.println("Регистрация клиента");
                    Network.getInstance().getCurrentChannel().writeAndFlush(buf);
                    logField.clear();
                    passField.clear();
                    nickField.clear();
                    window.close();
                }
            }
        });

        pane.getChildren().addAll(nickLabel, nickField, logLabel, logField, passLabel, passField, button);

        Scene scene = new Scene(pane, 170, 200);
        window.setResizable(false);
        window.setScene(scene);
        window.setTitle(title);
        window.showAndWait();

    }


}
