package base.command;

/**
 * Created by huoyijie on 18/10/22.
 */
public class Heartbeat {
    private String msg;

    public Heartbeat() {
    }

    public Heartbeat(String msg) {
        this.msg = msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "base.command.Heartbeat{" +
                "msg='" + msg + '\'' +
                '}';
    }
}
