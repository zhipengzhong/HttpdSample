package young.httpd;

import com.google.gson.Gson;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jws.WebResult;

import young.httpd.annotation.PathVariable;
import young.httpd.annotation.RequestHeader;
import young.httpd.annotation.RequestMapping;
import young.httpd.annotation.RequestParam;
import young.httpd.exception.NotAcceptableException;
import young.httpd.exception.NotFoundException;
import young.httpd.exception.UnauthorizedException;
import young.httpd.handler.HttpdHandler;
import young.httpd.multipart.MultipartFile;


public class HttpServer extends NanoHTTPD {

    private static final String POST = "POST";
    private static final String DELIMITER = "/";
    private static final int PORT = 8080;
    private static final Gson GSON = new Gson();

    protected List<Handler> mHandlers = new ArrayList<>();
    private ParameterMapping mParameterMapping;

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

    private void loadGeneratedHandler() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> clazz = Class.forName("young.httpd.GeneratedHttpdHandlerImpl");
        HttpdHandler httpdHandler = (HttpdHandler) clazz.newInstance();
        super.addHTTPInterceptor(httpdHandler);
    }

    /**
     * @param handler 处理器
     */
    public void addHandler(Object handler) {
        Class<?> aClass = handler.getClass();
        RequestMapping annotation = aClass.getAnnotation(RequestMapping.class);
        String value = annotation.value();
        Handler h = new Handler();
        h.setObject(handler);
        h.setRequestMapping(value);
        Method[] methods = aClass.getDeclaredMethods();
        for (Method method : methods) {
            RequestMapping methodAnnotation = method.getAnnotation(RequestMapping.class);
            if (methodAnnotation != null) {
                String s = methodAnnotation.value();
                if (s.indexOf(DELIMITER) != 0) {
                    s = DELIMITER + s;
                }
                h.addMethod(s, method);
            }
        }
        mHandlers.add(h);
    }

    @Override
    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        for (Handler handler : mHandlers) {
            String requestMapping = handler.getRequestMapping();
            Object object = handler.getObject();
            if (Pattern.matches("/?" + requestMapping + ".*", uri)) {
                String s = uri.substring(uri.indexOf(requestMapping) + requestMapping.length());
                if (s.indexOf(DELIMITER) != 0) {
                    s = DELIMITER + s;
                }
                HashMap<String, String> params = null;
                Method method = handler.getFixedMethod(s);
                if (method == null) {
                    params = new HashMap<>();
                    method = handler.getVariableMethod(s, params);
                }
                if (method != null) {
                    Object result;
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    int parameterLength = parameterTypes.length;
                    if (parameterLength <= 0) {
                        try {
                            result = method.invoke(object, new Object[]{});
                        } catch (InvocationTargetException e) {
                            return disposeFail(e);
                        } catch (Exception e) {
                            continue;
                        }
                    } else {

                        Map<String, String> file = null;
                        if (POST.equals(session.getMethod().name())) {
                            try {
                                file = new HashMap<>();
                                session.parseBody(file);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Map<String, String> parms = session.getParms();

                        Object[] objects = new Object[parameterLength];
                        Annotation[][] annotations = method.getParameterAnnotations();
                        for (int i = 0; i < parameterLength; i++) {
                            boolean isAdd = false;
                            Class<?> type = parameterTypes[i];
                            Annotation[] annotation = annotations[i];
                            for (Annotation an : annotation) {
                                if (RequestParam.class.isAssignableFrom(an.annotationType())) {
                                    objects[i] = disposeRequestParamParameter((RequestParam) an, session, file, parms, type);
                                    isAdd = true;
                                    break;
                                } else if (PathVariable.class.isAssignableFrom(an.annotationType())) {
                                    objects[i] = disposePathVariableParameter((PathVariable) an, session, file, parms, type, params);
                                    isAdd = true;
                                    break;
                                } else if (RequestHeader.class.isAssignableFrom(an.annotationType())) {
                                    objects[i] = disposeRequestHeaderParameter((RequestHeader) an, session, type);
                                    isAdd = true;
                                    break;
                                }
                            }
                            if (!isAdd) {
                                objects[i] = disposeDefaultParameter(session, file, parms, type);
                            }
                        }
                        try {
                            result = method.invoke(object, objects);
                        } catch (InvocationTargetException e) {
                            return disposeFail(e);
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    return disposeSucceed(result, method.getReturnType());
                }
            }
        }
        return disposeSucceed(super.handle(session), Response.class);
    }

    private Object disposeRequestHeaderParameter(RequestHeader requestHeader, IHTTPSession session, Class<?> type) {
        if (Map.class.isAssignableFrom(type)) {
            return session.getHeaders();
        }
        return null;
    }

    /**
     * 处理带PathVariable注解的参数
     *
     * @param param
     * @param session
     * @param file
     * @param parms
     * @param type
     * @param params
     * @return
     */
    private Object disposePathVariableParameter(PathVariable param, IHTTPSession session, Map<String, String> file, Map<String, String> parms, Class<?> type, Map<String, String> params) {
        String value = param.value();
        if (params != null && params.containsKey(value)) {
            String s = params.get(value);
            if (String.class.isAssignableFrom(type)) {
                return s;
            } else if (Integer.class.isAssignableFrom(type)) {
                return Integer.parseInt(s);
            }
        }
        return null;
    }


    /**
     * 处理带RequestParam注解的参数
     *
     * @param param
     * @param session
     * @param file
     * @param parms
     * @param type
     * @return
     */
    private Object disposeRequestParamParameter(RequestParam param, IHTTPSession session, Map<String, String> file, Map<String, String> parms, Class<?> type) {
        String value = param.value();
        if (MultipartFile.class.isAssignableFrom(type)) {
            if (file.containsKey(value)) {
                return new MultipartFile(new File(file.get(value)), null, null);
            }
        } else if (parms.containsKey(value)) {
            String s = parms.get(value);
            if (String.class.isAssignableFrom(type)) {
                return s;
            } else if (Integer.class.isAssignableFrom(type)) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                }

            }
            // TODO: 2020/3/16 待添加解析类型
        }
        return null;
    }

    /**
     * @param session
     * @param type
     * @return
     */
    private Object disposeDefaultParameter(IHTTPSession session, Map<String, String> file, Map<String, String> parms, Class<?> type) {
        if (IHTTPSession.class.isAssignableFrom(type)) {
            return session;
        } else if (Map.class.isAssignableFrom(type)) {
            return parms;
        } else if (NanoHTTPD.class.isAssignableFrom(type)) {
            return this;
        } else if (mParameterMapping != null) {
            return mParameterMapping.parameter(type);
        }
        return null;
    }


    /**
     * 处理成功响应
     *
     * @param result
     * @param resultType
     * @return
     */
    private Response disposeSucceed(Object result, Class resultType) {
        Response response;
        if (Response.class.isAssignableFrom(resultType)) {
            response = (Response) result;
        } else if (String.class.isAssignableFrom(resultType)) {
            response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, (String) result);
        } else if (WebResult.class.isAssignableFrom(resultType)) {
            response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, GSON.toJson(result));
        } else if (List.class.isAssignableFrom(resultType)) {
            response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, GSON.toJson(result));
        } else if (Map.class.isAssignableFrom(resultType)) {
            response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, GSON.toJson(result));
        } else {
            response = Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, "");
        }
        response.addHeader("Server", "YServer 1.0");
        return response;
    }


    /**
     * 处理失败响应
     *
     * @param e
     * @return
     */
    private Response disposeFail(InvocationTargetException e) {
        Throwable throwable = e.getCause();
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

    public void setParameterMapping(ParameterMapping mapping) {
        mParameterMapping = mapping;
    }

    public interface ParameterMapping {
        Object parameter(Class c);
    }

    private class Handler {
        private String mRequestMapping;
        private Object mObject;
        private HashMap<String, Method> mFixedMap = new HashMap<>();
        private HashMap<String, Method> mVariableMap = new HashMap<>();

        public String getRequestMapping() {
            return mRequestMapping;
        }

        public void setRequestMapping(String requestMapping) {
            mRequestMapping = requestMapping;
        }

        public Object getObject() {
            return mObject;
        }

        public void setObject(Object object) {
            mObject = object;
        }

        private void addMethod(String url, Method method) {
            List<String> list = matcher(url, "(?<=/|^)([^/]+)(?=/|$)");
            for (String s : list) {
                if (matcher(s, "(?<=^\\{)([a-zA-Z0-9_]+)(?=\\}$)").size() > 0) {
                    mVariableMap.put(url, method);
                    return;
                }
            }
            mFixedMap.put(url, method);
        }

        private Method getFixedMethod(String url) {
            return mFixedMap.get(url);
        }

        private Method getVariableMethod(String url, Map<String, String> params) {
            for (Map.Entry<String, Method> entry : mVariableMap.entrySet()) {
                String key = entry.getKey();
                List<String> methodList = matcher(key, "(?<=/|^)([^/]+)(?=/|$)");
                List<String> urlList = matcher(url, "(?<=/|^)([^/]+)(?=/|$)");
                if (methodList.size() != urlList.size()) {
                    continue;
                }
                for (int i = 0; i < methodList.size(); i++) {
                    List<String> matcher = matcher(methodList.get(i), "(?<=^\\{)([a-zA-Z0-9_]+)(?=\\}$)");
                    if (matcher.size() > 0) {
                        if (params != null)
                            params.put(matcher.get(0), urlList.get(i));
                    } else {
                        if (!methodList.get(i).equals(urlList.get(i))) {
                            params.clear();
                            continue;
                        }
                    }
                }
                return entry.getValue();
            }
            return null;
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
}
