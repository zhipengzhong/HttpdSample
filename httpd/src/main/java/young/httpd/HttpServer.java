package young.httpd;

import org.nanohttpd.protocols.http.NanoHTTPD;

import young.httpd.handler.HttpdHandler;


public class HttpServer extends NanoHTTPD {

    private static final int PORT = 8080;
    private HttpdHandler mHttpdHandler;

    public HttpServer() {
        this(PORT);
    }

    public HttpServer(int port) {
        super(port);
        try {
            loadGeneratedHandler();
        } catch (Exception e) {
        }
    }

    private void loadGeneratedHandler() {
        mHttpdHandler = new GeneratedHttpdHandlerImpl();
        super.addHTTPInterceptor(mHttpdHandler);
    }

    /**
     * 添加注入的参数
     *
     * @param o
     */
    public void addInject(Object o) {
        mHttpdHandler.addInject(o);
    }

    /**
     * 移除注入的参数
     *
     * @param o
     */
    public void removeInject(Object o) {
        mHttpdHandler.removeInject(o);
    }
}
