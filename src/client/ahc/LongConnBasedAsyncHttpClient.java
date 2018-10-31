package client.ahc;

import base.*;
import base.codec.CommandCodec;
import base.command.Command;
import base.command.CommandType;
import base.command.HttpRequest;
import base.command.HttpResponse;
import client.future.HttpFuture;
import client.listener.HttpFailureListener;
import client.listener.HttpSuccessListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import javax.net.ssl.SSLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by huoyijie on 18/10/17.
 */
// fixme retry or return error logic(when put not call or timeout)
public class LongConnBasedAsyncHttpClient implements AsyncHttpClient {
    private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());
    private final ExecutorService executorService = new ThreadPoolExecutor(2, 2,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100));
    private final ScheduledExecutorService timeoutExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final long timeout;

    private final AtomicLong nextReqId = new AtomicLong(1);

    private final ConcurrentHashMap<Long, HttpFuture> futureMap = new ConcurrentHashMap<>();

    private final long uniqueId;
    private final String host;
    private final int port;

    private final Bootstrap b = new Bootstrap();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private Channel channel;
    // Configure SSL.
    private SslContext sslCtx;

    public LongConnBasedAsyncHttpClient(String host, int port, long uniqueId) {
        this(host, port, uniqueId, Consts.DEFAULT_HTTP_REQUEST_TIMEOUT_MS);
    }

    public LongConnBasedAsyncHttpClient(String host, int port, long uniqueId, long timeout) {
        this.host = host;
        this.port = port;
        this.uniqueId = uniqueId;
        this.timeout = timeout;
    }

    @Override
    public void init() {
        AsyncHttpClient that = this;

        try {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            SslHandler sslHandler = sslCtx.newHandler(ch.alloc(), Consts.SERVER_HOST, Consts.SERVER_LISTEN_PORT);
                            sslHandler.handshakeFuture().addListener(future -> log.info("SSL Handshake Done."));
                            ch.pipeline()
                                    .addLast("logger", new LoggingHandler())
                                    .addLast(sslHandler)
                                    .addLast("idleHandler", new IdleStateHandler(
                                            Consts.R_IDLE_TIME, Consts.W_IDLE_TIME,
                                            Consts.RW_IDLE_TIME / 10, TimeUnit.SECONDS))
                                    .addLast("frameHandler", new LengthFieldBasedFrameDecoder(
                                            Consts.MAX_PACK_SIZE,
                                            Consts.LENGTH_FIELD_OFFSET,
                                            Consts.LENGTH_FIELD_LENGTH))
                                    .addLast("cmdCodec", new CommandCodec())
                                    .addLast("proxyHandler", new ClientProxyChannelHandler(that));
                        }
                    });
            connect();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                workerGroup.shutdownGracefully();
                log.info("shutdownGracefully.");
            }));
        } catch (Throwable cause) {
            log.error(cause);
        }
    }

    @Override
    public void connect() {
        b.connect(host, port).addListener((ChannelFuture futureFirst) -> {
            if (!futureFirst.isSuccess()) {
                log.warn("reconnected to server after 10s...");
                futureFirst.channel().eventLoop().schedule(() -> {
                    b.connect(host, port).addListener((ChannelFuture futureSecond) -> {
                        if(futureSecond.isSuccess()) {
                            channel = futureSecond.channel();
                            log.info("connected to server.");
                        } else {
                            log.error("connect to server failed.");
                            System.exit(-1);
                        }
                    });
                }, 10L, TimeUnit.SECONDS);
            } else {
                channel = futureFirst.channel();
                log.info("connected to server.");
            }
        });
    }

    @Override
    public void close() throws Exception {
        workerGroup.shutdownGracefully();
        log.info("shutdownGracefully.");
    }

    @Override
    public Future<HttpResponse> execute(HttpRequest req) {
        return __do__execute__(req, null, null);
    }

    @Override
    public void execute(HttpRequest req,
                        HttpSuccessListener successListener,
                        HttpFailureListener failureListener) {
        __do__execute__(req, successListener, failureListener);
    }

    private Future<HttpResponse> __do__execute__(HttpRequest req,
                                                 HttpSuccessListener successListener,
                                                 HttpFailureListener failureListener) {
        assert req != null;
        req.setReqId(nextReqId.getAndIncrement());
        HttpFuture future = new HttpFuture(successListener, failureListener);
        futureMap.putIfAbsent(req.getReqId(), future);
        channel.writeAndFlush(new Command<>(CommandType.http_request, uniqueId, req));
        timeoutExecutorService.schedule(() -> {
            if (!future.isDone()) {
                httpException(req.getReqId(), new RuntimeException("Http request timeout with " + req.getReqId()));
            }
        }, timeout, TimeUnit.MILLISECONDS);
        return future;
    }

    public void httpResponse(long reqId, HttpResponse resp) {
        assert reqId > 0;
        assert resp != null;
        HttpFuture future = futureMap.remove(reqId);
        if (future != null) {
            future.setResp(resp);
            if (future.getSuccessListener() != null) {
                executorService.execute(() -> future.getSuccessListener().callback(resp));
            }
        } else {
            log.error("When set http response, request not found with " + reqId);
        }
    }

    public void httpException(long reqId, Throwable cause) {
        assert cause != null;
        HttpFuture future = futureMap.remove(reqId);
        if (future != null) {
            future.setCause(cause);
            if (future.getFailureListener() != null) {
                executorService.execute(() -> future.getFailureListener().callback(cause));
            }
        } else {
            log.error("When set http exception, request not found with " + reqId);
        }
    }
}
