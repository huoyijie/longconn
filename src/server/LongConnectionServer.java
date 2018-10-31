package server;

import base.Consts;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import server.ahp.AsyncHttpProxy;

/**
 * Created by huoyijie on 18/10/15.
 */
public class LongConnectionServer {
    private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());

    private final int port;
    public LongConnectionServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        try (AsyncHttpProxy ahp = new AsyncHttpProxy(port)) {
            ahp.start();
        }
    }

    public static void main(String[] args) throws Exception {
        new LongConnectionServer(Consts.SERVER_LISTEN_PORT).run();
    }
}
