package client.ahc;

import base.*;
import base.command.*;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Created by huoyijie on 18/10/17.
 */
public class ClientProxyChannelHandler extends ChannelInboundHandlerAdapter {
    private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());
    private final AsyncHttpClient __this_ahc__;
    private static final Command CMD_HEARTBEAT =
            new Command<>(CommandType.heartbeat, 0L, new Heartbeat(Consts.HEARTBEAT_STR));

    public ClientProxyChannelHandler(AsyncHttpClient ahc) {
        this.__this_ahc__ = ahc;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("connected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Command cmd = (Command) msg;
        if (cmd.heartbeat()) {
            log.info(cmd.getContent().toString());
        } else if (cmd.getContent() instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) cmd.getContent();
            ((LongConnBasedAsyncHttpClient)__this_ahc__).httpResponse(response.getReqId(), response);
        } else if (cmd.getContent() instanceof PushRequest) {
            log.info(cmd.getContent().toString());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(CMD_HEARTBEAT)
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                    //// FIXME: 18/10/27 if failure reconnect
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            __this_ahc__.connect();
                        }
                    });
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //// FIXME: 18/10/27 reconnect
        __this_ahc__.connect();
    }
}
