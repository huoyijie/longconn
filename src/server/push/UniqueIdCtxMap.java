package server.push;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by huoyijie on 18/10/25.
 */
public class UniqueIdCtxMap {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(UniqueIdCtxMap.class);
    private static final ConcurrentHashMap<Long, ChannelHandlerContext> uniqueId2Ctx = new ConcurrentHashMap<>();

    public static void add(long uniqueId, ChannelHandlerContext ctx) {
        log.debug("server.push.UniqueIdCtxMap.bind " + uniqueId);
        if(!uniqueId2Ctx.containsKey(uniqueId)) {
            uniqueId2Ctx.putIfAbsent(uniqueId, ctx);
        }
    }

    public static void remove(long uniqueId) {
        log.debug("server.push.UniqueIdCtxMap.unbind " + uniqueId);
        uniqueId2Ctx.remove(uniqueId);
    }

    public static ChannelHandlerContext get(long uniqueId) {
        return uniqueId2Ctx.get(uniqueId);
    }
}
