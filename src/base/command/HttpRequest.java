package base.command;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by huoyijie on 18/10/22.
 */
// fixme not support cookie, also multipart, range etc...
public class HttpRequest {
    //unique request id, make sure not reach Long.max
    private long reqId;
    //request line
    private byte method;
    private String url;
    //header
    private Map<String, String> header = new LinkedHashMap<>();
    //body
    private byte[] body;

    public HttpRequest() {
    }

    public HttpRequest(byte method, String url) {
        this.method = method;
        this.url = url;
    }

    public long getReqId() {
        return reqId;
    }

    public HttpRequest setReqId(long reqId) {
        this.reqId = reqId;
        return this;
    }

    public byte getMethod() {
        return method;
    }

    public void setMethod(byte method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public HttpRequest addHeader(String key, String value) {
        header.put(key, value);
        return this;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public HttpRequest addBody(byte[] bytes) {
        this.body = bytes;
        return this;
    }

    @Override
    public String toString() {
        return "base.command.HttpRequest{" +
                "reqId=" + reqId +
                ", method=" + method +
                ", url='" + url + '\'' +
                ", header=" + header +
                ", body=" + (body != null ? body.length : "") +
                '}';
    }
}
