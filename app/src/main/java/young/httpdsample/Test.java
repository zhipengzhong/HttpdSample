package young.httpdsample;

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

import young.httpd.annotation.PathVariable;
import young.httpd.annotation.RequestHeader;
import young.httpd.annotation.RequestMapping;

@RequestMapping("test")
public class Test {

    @RequestMapping("test")
    public String test() {
        return null;
    }

    @RequestMapping("table/{id}/")
    public Map deviceTable(@PathVariable("id") String id, @PathVariable("id1") String id1, MainActivity activity) {
        System.out.println(id);
        System.out.println(id1);
        System.out.println(activity);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("11", "22");
        map.put("33", "44");
        return map;
    }

    @RequestMapping("inject")
    public String testInject(Long time) {
        if (time != null) {
            long l = (System.currentTimeMillis() - time) / 1000;
            return "对象回收测试   time:" + l;
        }
        return "对象已回收";
    }


    @RequestMapping("info3")
    public void info2(@PathVariable("deviceID") String deviceID, @RequestHeader Map<String, String> headers) {
    }

    @RequestMapping("info1")
    public void info1(AsyncTask<Void, String, VerifyError> verifyErrorAsyncTask) {
    }
}
