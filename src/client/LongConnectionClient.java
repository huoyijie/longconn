package client;

import base.Consts;
import base.GZipUtil;
import base.command.HttpRequest;
import client.ahc.AsyncHttpClient;
import client.listener.HttpListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

/**
 * Created by huoyijie on 18/10/16.
 */
public class LongConnectionClient {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(LongConnectionClient.class);
    private static final long UNIQUE_ID = 10000L;
    private final String host;
    private final int port;
    private final long uniqueId;

    public LongConnectionClient(String host, int port, long uniqueId) {
        this.host = host;
        this.port = port;
        this.uniqueId = uniqueId;
    }

    public void run() throws Exception {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

        try (AsyncHttpClient ahc = AsyncHttpClient.newInstance(host, port, uniqueId)) {
            ahc.init();

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String req;
            while ((req = br.readLine()) != null) {
                if (req.isEmpty()) continue;
                if ("exit".equalsIgnoreCase(req)) break;

                HttpRequest request = new HttpRequest(Consts.HTTP_METHOD_GET, req);
                request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.67 Safari/537.36")
                        .addHeader("Token", req)
                        .addBody("http body test".getBytes());
                ahc.execute(request, response -> {
                    //// FIXME: 18/10/22 body use default charset
                    //// FIXME: 18/10/23 ungzip should not appear here
                    //// FIXME: 18/10/23 test speed on gzip and no gzip
                    byte[] contents;
                    if ((contents = GZipUtil.uncompress(response.getBody())) != null) {
                        String body = new String(contents);
                        log.info("len before gzip:" + body.length());
                        log.info("len after gzip:" + response.getBody().length);
                    } else {
                        log.info(response.getReqId() + "'body blank");
                    }
                }, HttpListener.ONLY_LOG_FAILURE);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new LongConnectionClient(
                Consts.SERVER_HOST,
                Consts.SERVER_LISTEN_PORT, UNIQUE_ID).run();
    }
}