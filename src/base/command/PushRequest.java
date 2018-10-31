package base.command;

/**
 * Created by huoyijie on 18/10/25.
 */
public class PushRequest {

    private String content;

    public PushRequest() {
    }

    public PushRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "base.command.PushRequest{" +
                "content='" + content + '\'' +
                '}';
    }
}
