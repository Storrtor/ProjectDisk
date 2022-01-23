package fx.client;

import common.Constants;
import common.ProtoHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.swing.*;

public class ClientAuthHandler extends ChannelInboundHandlerAdapter {

    private static boolean authOk = false;
    private static String nick;
    private int nextLength;

    public static String getNick() {
        return nick;
    }

    public static boolean isAuthOk() {
        return authOk;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (authOk) {
            ctx.fireChannelRead(msg);
        } else {
            ByteBuf buf = ((ByteBuf) msg);
            // Если пользователь авторизирован, отправляем посылку дальше
            byte authByte = buf.readByte();
            if (authByte == Constants.AUTH_OK) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get nick length");
                    nextLength = buf.readInt();
                }
                if (buf.readableBytes() >= nextLength) {
                    System.out.println("STATE: Get nick");
                    byte[] nickByte = new byte[nextLength];
                    buf.readBytes(nickByte);
                    nick = new String(nickByte); //
                    authOk = true;
                    ctx.pipeline().addLast(new ProtoHandler(("client_storage/" + ClientAuthHandler.getNick()), new ClientCommandReceiver()));
                    Network.sendMsg(() -> {
                        JOptionPane.showConfirmDialog(null, "Авторизация прошла успешно");
                    });
                }
            }
            if (authByte == Constants.AUTH_NOT_OK) {
                Network.sendMsg(() -> {
                    JOptionPane.showConfirmDialog(null, "Неверные логин или пароль");
                });
            }
            if (authByte == Constants.REG_NOT_OK) {
                Network.sendMsg(() -> {
                    JOptionPane.showConfirmDialog(null, "Никнейм или логин уже используются");
                });
            }
            if (authByte == Constants.REG_OK) {
                Network.sendMsg(() -> {
                    JOptionPane.showConfirmDialog(null, "Регистрация прошла успешно. Для продолжения работы авторизируйтесь.");
                });
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
