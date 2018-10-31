package base;

/**
 * Created by huoyijie on 18/10/17.
 */
public class Consts {
    public static final int MAX_PACK_SIZE = 1024 * 1024;
    public static final int LENGTH_FIELD_OFFSET = 0;
    public static final int LENGTH_FIELD_LENGTH = 4;
    public static final int TYPE_FIELD_LENGTH = 1;
    public static final int UNIQUE_ID_FIELD_LENGTH = 8;

    public static final int CHANNEL_OPTION_SO_BACKLOG_SIZE = 128;

    public static final String SERVER_HOST = "127.0.0.1";
    public static final int SERVER_LISTEN_PORT = 9999;

    public static final int RW_IDLE_TIME = 60;
    public static final int R_IDLE_TIME = 0;
    public static final int W_IDLE_TIME = 0;

    public static final String HEARTBEAT_STR = "HEARTBEAT";

    public static final byte HTTP_METHOD_GET = 1;
    public static final byte HTTP_METHOD_POST = 2;

    public static final long DEFAULT_HTTP_REQUEST_TIMEOUT_MS = 5000L;
}
