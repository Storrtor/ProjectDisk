package protocol;

import common.Constants;
import common.ProtoHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private static String nick;
    private int nextLength;
    private boolean authOkBol = false;

    public static String getNick() {
        return nick;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg);
        // Если пользователь авторизирован, отправляем посылку дальше
        if (authOkBol) {
            ctx.fireChannelRead(buf);
        } else {
            // Иначе получаем длину сообщения и вычитываем его в стринг
            byte readByte = buf.readByte();
            //  БЛОК АВТОРИЗАЦИИ
            if (readByte == Constants.AUTH_NOT_OK) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get auth length");
                    nextLength = buf.readInt();
                }
                if (buf.readableBytes() >= nextLength) {
                    byte[] strAuth = new byte[nextLength];
                    buf.readBytes(strAuth);
                    String authStr = new String(strAuth, StandardCharsets.UTF_8);
                    try {
                        // Идем в бд и авторизируем пользователя
                        String[] parts = authStr.split("\\s+");
                        nick = Server.getAuthService().getNickByLoginAndPass(parts[0], parts[1]);
                        System.out.println("Залогинились");
                        if (nick != null) {
                            // Отправляем на клиента сигнал, что пользователь успешно прошел авторизацию
                            // И его полученный из бд никнейм
                            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + nick.getBytes(StandardCharsets.UTF_8).length);
                            byteBuf.writeByte(Constants.AUTH_OK);
                            byteBuf.writeInt(nick.length());
                            byteBuf.writeBytes(nick.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(byteBuf);
                            authOkBol = true;
                            // добавляем в пайп лайн хендлер для файлов и команд
                            ctx.pipeline().addLast(
                                    new ProtoHandler("server_storage/" + AuthHandler.getNick(), new ServerCommandReceiver()));
                            return;
                        }
                        // если авторизация не выполнилась успешно, отправляем сигнал об этом на клиент
                    } catch (ArrayIndexOutOfBoundsException | SQLException ex) {
                        ByteBuf byteBuf = ctx.alloc().buffer(1);
                        byteBuf.writeByte(Constants.AUTH_NOT_OK);
                        ctx.writeAndFlush(byteBuf);
                        System.out.println("Логин/пароль не прошел");
                    }
                }
            }

            // БЛОК РЕГИСТРАЦИИ
            if (readByte == Constants.COMMAND_SIGN_IN) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get registr length");
                    nextLength = buf.readInt();
                }
                if (buf.readableBytes() >= nextLength) {
                    byte[] strAuth = new byte[nextLength];
                    buf.readBytes(strAuth);
                    String authStr = new String(strAuth, StandardCharsets.UTF_8);
                    try {
                        // Идем в бд и регистрируем пользователя + создаем ему папки
                        String[] parts = authStr.split("\\s+");
                        // пробуем регистрировать пользователя, если он вводит уже существующий логин или пароль,
                        // посылаем на клиента сигнал, что такой пользователь уже существует
                        Server.getAuthService().insert(parts[0], parts[1], parts[2]);
                        nick = parts[0];
                        System.out.println("Зарегистрировались");
                        if (nick != null) {
                            File fileClient = new File("client_storage/" + nick);
                            File fileServer = new File("server_storage/" + nick);
                            if (!fileClient.exists()) {
                                fileClient.mkdir();
                            }
                            if (!fileServer.exists()) {
                                fileServer.mkdir();
                            }
                            System.out.println("Создали папки");
                            // Отправляем на клиента сигнал, что пользователь успешно прошел авторизацию
                            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(1);
                            byteBuf.writeByte(Constants.REG_OK);
                            System.out.println("Регистрация ок");
                            ctx.writeAndFlush(byteBuf); //отправка обратно
                            return;
                        }
                    } catch (SQLException ex) {
                        ByteBuf byteBuf = ctx.alloc().buffer(1);
                        byteBuf.writeByte(Constants.REG_NOT_OK);
                        ctx.writeAndFlush(byteBuf);
                        System.out.println("Логин/пароль не прошел");
                    }
                }
            }
            if (buf.readableBytes() == 0) {
                buf.release();
            }
        }
    }

}

