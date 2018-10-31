package server.push;

import base.command.Command;
import base.command.CommandType;
import base.command.PushRequest;

/**
 * Created by huoyijie on 18/10/25.
 */
public class PushAgent {
    public static final Command<PushRequest> TEST_PUSH =
            new Command<>(CommandType.push_request, 10000L,
                    new PushRequest("Test Push"));

    public static final boolean test() {
        UniqueIdCtxMap.get(10000L).writeAndFlush(PushAgent.TEST_PUSH);
        return true;
    }
}
