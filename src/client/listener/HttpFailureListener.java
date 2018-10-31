package client.listener;

/**
 * Created by huoyijie on 18/10/26.
 */
@FunctionalInterface
public interface HttpFailureListener extends HttpListener {
    void callback(Throwable cause);
}
