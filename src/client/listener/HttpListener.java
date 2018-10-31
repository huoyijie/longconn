package client.listener;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Created by huoyijie on 18/10/26.
 */
public interface HttpListener {
    InternalLogger log = InternalLoggerFactory.getInstance(HttpListener.class);
    HttpSuccessListener ONLY_LOG_SUCCESS = response -> log.info(response.toString());
    HttpFailureListener ONLY_LOG_FAILURE = log::info;
}
