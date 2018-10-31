package base.command;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by huoyijie on 18/10/22.
 */
public class HttpResponse {
    //unique request id, which comes from base.command.HttpRequest obj
    private long reqId;
    //response line
    private int statusCode;
    private String statusText;
    //header
    private Map<String, String> header = new LinkedHashMap<>();
    //body
    private byte[] body;

    public HttpResponse() {
    }

    public HttpResponse(long reqId) {
        this.reqId = reqId;
    }

    public long getReqId() {
        return reqId;
    }

    public void setReqId(long reqId) {
        this.reqId = reqId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return body;
    }

    public HttpResponse addHeader(String key, String value) {
        header.put(key, value);
        return this;
    }

    public HttpResponse addBody(byte[] bytes) {
        this.body = bytes;
        return this;
    }

    public HttpResponse addStatus(int statusCode, String statusText) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        return this;
    }

    @Override
    public String toString() {
        return "base.command.HttpResponse{" +
                "reqId=" + reqId +
                ", statusCode=" + statusCode +
                ", statusText='" + statusText + '\'' +
                ", header=" + header +
                ", body=" + (body != null ? body.length : "") +
                '}';
    }
}
