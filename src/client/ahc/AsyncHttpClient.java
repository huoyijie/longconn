package client.ahc;

import base.command.HttpRequest;
import base.command.HttpResponse;
import client.listener.HttpFailureListener;
import client.listener.HttpSuccessListener;

import java.util.concurrent.Future;

/**
 * Created by huoyijie on 18/10/26.
 */
public interface AsyncHttpClient extends AutoCloseable {

    void init();

    void connect();

    Future<HttpResponse> execute(HttpRequest request);

    void execute(HttpRequest request,
                        HttpSuccessListener successListener,
                        HttpFailureListener failureListener);

    static AsyncHttpClient newInstance(String host, int port, long uniqueId) {
        return new LongConnBasedAsyncHttpClient(host, port, uniqueId);
    }
}
