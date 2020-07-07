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
        } catch (Throwable e) {
            e.printStackTrace();
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
    public void inject(Object o) {
        mHttpdHandler.inject(o);
    }
}
