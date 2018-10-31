package server.ahp;

import base.*;
import base.command.*;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import server.push.UniqueIdCtxMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by huoyijie on 18/10/17.
 */
public class ServerProxyChannelHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(ServerProxyChannelHandler.class);

    private static final Command CMD_HEARTBEAT =
            new Command<>(CommandType.heartbeat, 0L, new Heartbeat(Consts.HEARTBEAT_STR));

    private final AsyncHttpProxy __this_ahp__;
    public ServerProxyChannelHandler(AsyncHttpProxy ahp) {
        __this_ahp__ = ahp;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Command cmd = (Command) msg;
        if (cmd.heartbeat()) {
            log.info(cmd.getContent().toString());
        } else if (cmd.getContent() instanceof HttpRequest) {
            bindUniqueID(ctx, cmd.getUniqueId());
            HttpRequest request = (HttpRequest) cmd.getContent();
            RequestBuilder.build(__this_ahp__.getAsyncHttpClient(), request)
                          .execute()
                          .toCompletableFuture()
                          //// FIXME: 18/10/22 need return body and header
                          .thenAccept(res -> {
                              //log.info(res);
                              HttpResponse response = new HttpResponse(request.getReqId());
                              response.addStatus(res.getStatusCode(), res.getStatusText())
                                      .addBody(GZipUtil.compress(res.getResponseBody()));
                              res.getHeaders().forEach(kv -> response.addHeader(kv.getKey(), kv.getValue()));
                              //// FIXME: 18/10/23 request body also gzip, gzip should set in response header
                              ctx.writeAndFlush(new Command<>(CommandType.http_response, cmd.getUniqueId(), response));
                              //log.info(response);
                          });
                }
    }

    private void bindUniqueID(ChannelHandlerContext ctx, long uniqueId) {
        if (uniqueId > 0) {
            UniqueIdCtxMap.add(uniqueId, ctx);
            AttributeKey<Long> attrKey = AttributeKey.valueOf("UniqueID");
            if (!ctx.channel().hasAttr(attrKey)) {
                ctx.channel().attr(attrKey).set(uniqueId);
            }
        }
    }

    private void unbindUniqueID(ChannelHandlerContext ctx) {
        AttributeKey<Long> attrKey = AttributeKey.valueOf("UniqueID");
        Long uniqueId = ctx.channel().attr(attrKey).get();
        if (uniqueId != null && uniqueId > 0L) {
            UniqueIdCtxMap.remove(uniqueId);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(CMD_HEARTBEAT)
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        unbindUniqueID(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause.getMessage());
        ctx.close();
    }

    private static final class RequestBuilder {
        private static final InternalLogger log = InternalLoggerFactory.getInstance(RequestBuilder.class);
        private static final Set<Byte> supportMethodSet = new HashSet<>();
        static {
            supportMethodSet.add(Consts.HTTP_METHOD_GET);
            supportMethodSet.add(Consts.HTTP_METHOD_POST);
        }
        public static final BoundRequestBuilder build(AsyncHttpClient asyncHttpClient, HttpRequest request) {
            log.info(request.toString());
            //// FIXME: 18/10/22 only support get/post
            assert supportMethodSet.contains(request.getMethod());
            String method = (request.getMethod() == Consts.HTTP_METHOD_GET) ? HttpMethod.GET.name() : HttpMethod.POST.name();
            BoundRequestBuilder requestBuilder = asyncHttpClient.prepare(method, request.getUrl());
            if (request.getBody() != null) {
                requestBuilder.setBody(request.getBody());
            }
            request.getHeader().forEach(requestBuilder::addHeader);
            return requestBuilder;
        }
    }
}