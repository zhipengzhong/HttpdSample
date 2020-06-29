package young.httpd.handler;

import java.util.List;
import java.util.Map;

public abstract class HttpdHandler {

    protected void registerUrl(String url) {
        throw new RuntimeException("Stub!");
    }

    protected abstract Object disposeRequest(String url, List<Object> inject, Map<String, String> params, Map<String, String> files, Map<String, String> paths, Map<String, String> headers);

    public void addInject(Object o) {
        throw new RuntimeException("Stub!");
    }

    public void removeInject(Object o) {
        throw new RuntimeException("Stub!");
    }
}
