package server.ahp;

import base.Consts;
import base.codec.CommandCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.asynchttpclient.AsyncHttpClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.asynchttpclient.Dsl.asyncHttpClient;

/**
 * Created by huoyijie on 18/10/26.
 */
public class AsyncHttpProxy implements AutoCloseable {
    private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());
    private final AsyncHttpClient asyncHttpClient = asyncHttpClient();
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public AsyncHttpClient getAsyncHttpClient() {
        return asyncHttpClient;
    }
    private final int port;
    public AsyncHttpProxy(int port) {
        this.port = port;
    }

    public void start() {
        AsyncHttpProxy that = this;

        try {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            SslHandler sslHandler = sslCtx.newHandler(ch.alloc());
                            sslHandler.handshakeFuture().addListener(future -> log.info("SSL Handshake Done."));
                            ch.pipeline()
                                    .addLast("logger", new LoggingHandler())
                                    .addLast(sslHandler)
                                    .addLast("idleHandler", new IdleStateHandler(
                                            Consts.R_IDLE_TIME, Consts.W_IDLE_TIME,
                                            Consts.RW_IDLE_TIME, TimeUnit.SECONDS))
                                    .addLast("frameHandler", new LengthFieldBasedFrameDecoder(
                                            Consts.MAX_PACK_SIZE,
                                            Consts.LENGTH_FIELD_OFFSET,
                                            Consts.LENGTH_FIELD_LENGTH))
                                    .addLast("cmdCodec", new CommandCodec())
                                    .addLast("proxyHandler", new ServerProxyChannelHandler(that));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, Consts.CHANNEL_OPTION_SO_BACKLOG_SIZE)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            b.bind(port).addListener(future -> {
                if(future.isSuccess()) {
                    log.info("server start, listen on port " + port);
                } else {
                    log.error("server start failed.");
                }
            }).channel().closeFuture().addListener(future -> log.info("server channel closed."));
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            bossGroup.terminationFuture().sync();
        } catch (Throwable cause) {
            log.error(cause);
        }
    }

    private void shutdown() {
        try {
            close();
        } catch (Throwable cause) {
            log.error(cause);
        }
    }

    @Override
    public void close() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        //// FIXME: 18/10/18 must be closed
        try {
            asyncHttpClient.close();
        } catch (IOException e) {
            log.error(e);
        }
        log.info("shutdownGracefully...");
    }
}
