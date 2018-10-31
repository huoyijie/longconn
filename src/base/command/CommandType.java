package base.command;

/**
 * Created by huoyijie on 18/10/22.
 */
public enum CommandType {
    none((byte) -1),
    heartbeat((byte) 0),
    http_request((byte) 1),
    http_response((byte) 2),
    push_request((byte) 3);

    private byte value;

    public byte getValue() {
        return value;
    }

    CommandType(byte value) {
        this.value = value;
    }

    public static CommandType byValue(byte type) {
        for(CommandType commandType: CommandType.values()) {
            if (commandType.getValue() == type) return commandType;
        }
        return CommandType.none;
    }
}
