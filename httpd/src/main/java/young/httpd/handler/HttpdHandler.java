package young.httpd.handler;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.util.IHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import young.httpd.multipart.MultipartFile;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class HttpdHandler implements IHandler<IHTTPSession, Response> {

    private List<String> mUrlList = new ArrayList<>();

    protected void registerUrl(String url) {
        mUrlList.add(url);
    }

    protected abstract Object disposeRequest(String url, List<Object> inject, Map<String, String> params, Map<String, String> files, Map<String, String> paths, Map<String, String> headers) throws Exception;

    @Override
    public Response handle(IHTTPSession input) {
        return null;
    }

    protected <T> T getRequestHeader(String key, Map<String, String> headers, Class<T> clazz) {
        if (key == null || key.length() <= 0) {
            if (Map.class.isAssignableFrom(clazz)) {
                return (T) headers;
            }
            return null;
        }
        if (headers.containsKey(key)) {
            String s = headers.get(key);
            if (String.class.isAssignableFrom(clazz)) {
                return (T) s;
            } else if (Integer.class.isAssignableFrom(clazz)) {
                return (T) Integer.valueOf(Integer.parseInt(s));
            }
        }
        return null;
    }

    protected <T> T getPathVariable(String key, Map<String, String> paths, Class<T> clazz) {
        if (key == null || key.length() <= 0) return null;
        if (paths != null && paths.containsKey(key)) {
            String s = paths.get(key);
            if (String.class.isAssignableFrom(clazz)) {
                return (T) s;
            } else if (Integer.class.isAssignableFrom(clazz)) {
                return (T) Integer.valueOf(Integer.parseInt(s));
            }
        }
        return null;
    }

    protected static <T> T getRequestParam(String key, Map<String, String> params, Map<String, String> files, Class<T> clazz) {
        if (key == null || key.length() <= 0) return null;
        if (MultipartFile.class.isAssignableFrom(clazz)) {
            if (files.containsKey(key)) {
                return (T) new MultipartFile(new File(files.get(key)), null, null);
            }
        } else if (params.containsKey(key)) {
            String s = params.get(key);
            if (String.class.isAssignableFrom(clazz)) {
                return (T) s;
            } else if (Integer.class.isAssignableFrom(clazz)) {
                try {
                    return (T) Integer.valueOf(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                }
            }
            // TODO: 2020/3/16 待添加解析类型
        }
        return null;
    }

    protected static <T> T getInject(List<Object> inject, Class<T> clazz) {
        if (inject == null) return null;
        for (Object o : inject) {
            if (o.getClass().isAssignableFrom(clazz)) {
                return (T) o;
            }
        }
        return null;
    }

    protected static final int hash(String key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
}
