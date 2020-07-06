package young.httpd.handler;

import com.google.gson.Gson;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.util.IHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import young.httpd.exception.NotAcceptableException;
import young.httpd.exception.NotFoundException;
import young.httpd.exception.UnauthorizedException;
import young.httpd.multipart.MultipartFile;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class HttpdHandler implements IHandler<IHTTPSession, Response> {

    private static final String POST = "POST";
    private static final Gson GSON = new Gson();
    private List<String> mFixedUrlList = new ArrayList<>();
    private List<String> mVariableUrlList = new ArrayList<>();
    private List<Object> mInjectList = new CopyOnWriteArrayList<>();

    protected void registerUrl(String url) {
        List<String> list = matcher(url, "(?<=/|^)([^/]+)(?=/|$)");
        for (String s : list) {
            if (matcher(s, "(?<=^\\{)([a-zA-Z0-9_]+)(?=\\}$)").size() > 0) {
                mVariableUrlList.add(url);
                return;
            }
        }
        mFixedUrlList.add(url);
    }

    protected abstract Object disposeRequest(String url, List<Object> inject, Map<String, String> params, Map<String, String> files, Map<String, String> paths, Map<String, String> headers);

    @Override
    public Response handle(IHTTPSession input) {
        String requestUrl = input.getUri().replaceAll("^/|/$", "");
        HashMap<String, String> pathVariable = new HashMap<>();
        String mapping = getMappingUrl(requestUrl, pathVariable);
        if (mapping == null) return null;
        Map<String, String> headers = input.getHeaders();

        Map<String, String> file = null;
        if (POST.equals(input.getMethod().name())) {
            try {
                file = new HashMap<>();
                input.parseBody(file);
            } catch (Exception e) {
            }
        }
        Map<String, String> params = input.getParms();

        try {
            return disposeSucceed(disposeRequest(mapping, mInjectList, params, file, pathVariable, headers));
        } catch (Exception e) {
            return disposeFail(e);
        }
    }

    /**
     * 添加注入的参数
     *
     * @param o
     */
    public void addInject(Object o) {
        mInjectList.add(o);
    }

    /**
     * 移除注入的参数
     *
     * @param o
     */
    public void removeInject(Object o) {
        mInjectList.remove(o);
    }


    /**
     * 处理成功响应
     *
     * @param result
     * @return
     */
    private Response disposeSucceed(Object result) {
        Response response;
        if (result == null) {
            response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, "");
        } else {
            Class resultType = result.getClass();
            if (Response.class.isAssignableFrom(resultType)) {
                response = (Response) result;
            } else if (String.class.isAssignableFrom(resultType)) {
                response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, (String) result);
            } else if (List.class.isAssignableFrom(resultType)) {
                response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, GSON.toJson(result));
            } else if (Map.class.isAssignableFrom(resultType)) {
                response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, GSON.toJson(result));
            } else {
                response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, GSON.toJson(result));
            }
        }
        response.addHeader("Server", "YServer 1.0");
        return response;
    }


    /**
     * 处理失败响应
     *
     * @param throwable
     * @return
     */
    private Response disposeFail(Exception throwable) {
        Response response;
        if (throwable instanceof UnauthorizedException) {
            response = Response.newFixedLengthResponse(Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, "");
        } else if (throwable instanceof NotAcceptableException) {
            response = Response.newFixedLengthResponse(Status.NOT_ACCEPTABLE, NanoHTTPD.MIME_PLAINTEXT, "");
        } else if (throwable instanceof NotFoundException) {
            response = Response.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "");
        } else {
            response = Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "");
        }
        response.addHeader("Server", "YServer 1.0");
        return response;
    }

    /**
     * 获取映射的URL
     *
     * @param requestUrl
     * @param params
     * @return
     */
    private String getMappingUrl(String requestUrl, HashMap<String, String> params) {
        String mappingUrl = null;
        if (mFixedUrlList.contains(requestUrl)) {
            return requestUrl;
        }
        if (mappingUrl == null) {
            for (String variable : mVariableUrlList) {
                List<String> mappingList = matcher(variable, "(?<=/|^)([^/]+)(?=/|$)");
                List<String> urlList = matcher(requestUrl, "(?<=/|^)([^/]+)(?=/|$)");
                if (mappingList.size() != urlList.size()) {
                    continue;
                }
                boolean isMatch = true;
                for (int i = 0; i < mappingList.size(); i++) {
                    List<String> matcher = matcher(mappingList.get(i), "(?<=^\\{)([a-zA-Z0-9_]+)(?=\\}$)");
                    if (matcher.size() > 0) {
                        params.put(matcher.get(0), urlList.get(i));
                    } else {
                        if (!mappingList.get(i).equals(urlList.get(i))) {
                            params.clear();
                            isMatch = false;
                            break;
                        }
                    }
                }
                if (isMatch) {
                    return variable;
                }
            }
        }
        return null;
    }

    protected <T> T getRequestHeader(String key, Map<String, String> headers, Class<T> clazz) {
        if (key == null || key.length() <= 0) {
            if (Map.class.isAssignableFrom(clazz)) {
                return (T) headers;
            }
            return null;
        }
        if (headers != null && headers.containsKey(key)) {
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
        if (key == null) return null;
        if (files != null && MultipartFile.class.isAssignableFrom(clazz)) {
            if (files.containsKey(key)) {
                return (T) new MultipartFile(new File(files.get(key)), null, null);
            }
        } else if (params != null && key.length() > 0 && params.containsKey(key)) {
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
        } else if (params != null && "".equals(key)) {
            if (Map.class.isAssignableFrom(clazz)) {
                return (T) params;
            }
        }
        return null;
    }

    protected static <T> T getInject(List<Object> inject, Class<T> clazz) {
        if (inject == null) return null;
        for (Object o : inject) {
            if (clazz.isAssignableFrom(o.getClass())) {
                return (T) o;
            }
        }
        return null;
    }

    protected static final int hash(String key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    private List<String> matcher(String src, String regex) {
        ArrayList<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(src);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }
}
