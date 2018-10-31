package base.command;

import base.Consts;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import java.io.IOException;

/**
 * Created by huoyijie on 18/10/22.
 */
public class Command<T> {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(Command.class);
    //0->heartbeat, 1->http request, 2->http response, 3->push etc...
    private byte type;
    //server generated
    private long uniqueId;
    private T content;

    public Command(CommandType commandType, long uniqueId, T content) {
        this.type = commandType.getValue();
        this.uniqueId = uniqueId;
        this.content = content;
    }

    public boolean heartbeat() {
        return CommandType.heartbeat == CommandType.byValue(type);
    }

    public byte getType() {
        return type;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public T getContent() {
        return content;
    }

    public static ByteBuf out(Command<?> cmd) {
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(cmd.getContent());
            assert bytes != null && bytes.length > 0;
            int length = Consts.TYPE_FIELD_LENGTH/*type field*/
                    + Consts.UNIQUE_ID_FIELD_LENGTH/*uniqueId field*/
                    + bytes.length/*content field*/;
            int packSize = Consts.LENGTH_FIELD_LENGTH/*length field*/ + length;

            ByteBuf buf = Unpooled.buffer(packSize);
            buf.writeInt(length);
            buf.writeByte(cmd.getType());
            buf.writeLong(cmd.getUniqueId());
            buf.writeBytes(bytes);
            buf = Unpooled.wrappedUnmodifiableBuffer(buf);
            return buf;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("SerialError");
        }
    }

    public static Command<?> in(ByteBuf buf) {
        assert buf != null && buf.readableBytes() > 0;

        if (buf.isReadable(Consts.LENGTH_FIELD_LENGTH)) {
            //// FIXME: 18/10/22 readInt may cause bugs when LENGTH_FIELD_LENGTH changed
            int length = buf.readInt();
            int headerLen = Consts.TYPE_FIELD_LENGTH + Consts.UNIQUE_ID_FIELD_LENGTH;
            assert length > headerLen;
            if (buf.isReadable(length)) {
                byte type = buf.readByte();
                long uniqueId = buf.readLong();
                byte[] bytes = new byte[length - headerLen];
                buf.readBytes(bytes);
                assert bytes != null && bytes.length > 0;

                ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
                try {
                    switch (CommandType.byValue(type)) {
                        case heartbeat: {
                            return new Command<>(CommandType.heartbeat,
                                    uniqueId,
                                    objectMapper.readValue(bytes, Heartbeat.class));
                        }
                        case http_request: {
                            return new Command<>(CommandType.http_request,
                                    uniqueId,
                                    objectMapper.readValue(bytes, HttpRequest.class));
                        }
                        case http_response: {
                            return new Command<>(CommandType.http_response,
                                    uniqueId,
                                    objectMapper.readValue(bytes, HttpResponse.class));
                        }
                        case push_request: {
                            return new Command<>(CommandType.push_request,
                                    uniqueId,
                                    objectMapper.readValue(bytes, PushRequest.class));
                        }
                        default: {
                            throw new RuntimeException("base.command.Command.type invalid!");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                log.error("Decode Error!Need process");
            }
        } else {
            log.error("Decode Error!Need process");
        }
        return null;
    }

    @Override
    public String toString() {
        return "base.command.Command{" +
                "type=" + type +
                ", uniqueId='" + uniqueId + '\'' +
                ", content=" + content +
                '}';
    }
}
