package fx.client;

import common.CommandReceiver;
import io.netty.channel.ChannelHandlerContext;

public class ClientCommandReceiver extends CommandReceiver {

    @Override
    public void parseCommand(ChannelHandlerContext ctx, String cmd) {
        throw new IllegalStateException("У клиента нет команд");
    }

}
