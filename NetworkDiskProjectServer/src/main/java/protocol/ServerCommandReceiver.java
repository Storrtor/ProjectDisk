package protocol;

import common.CommandReceiver;
import common.ProtoFileSender;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.nio.file.Paths;


public class ServerCommandReceiver extends CommandReceiver {

    @Override
    public void parseCommand(ChannelHandlerContext ctx, String cmd) throws Exception {
        if (cmd.startsWith("/request ")) {
            String fileToClientName = cmd.split("\\s")[1];
            System.out.println(AuthHandler.getNick());
            ProtoFileSender.sendFile(Paths.get(("server_storage/" + AuthHandler.getNick()), fileToClientName), ctx.channel(), null);
        }
        if (cmd.startsWith("/delete ")) {
            String fileToDeleteName = cmd.split("\\s")[1];
            File file = new File("server_storage/" + AuthHandler.getNick() + "/" + fileToDeleteName);
            file.delete();
        }
    }
}
