package client.listener;

import base.command.HttpResponse;

/**
 * Created by huoyijie on 18/10/25.
 */
@FunctionalInterface
public interface HttpSuccessListener extends HttpListener {
    void callback(HttpResponse response);
}
