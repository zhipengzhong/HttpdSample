package young.httpdsample;

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

import young.httpd.annotation.PathVariable;
import young.httpd.annotation.RequestHeader;
import young.httpd.annotation.RequestMapping;
import young.httpd.annotation.RequestParam;

@RequestMapping("admin/v1/api/test/")
public class Test {

    @RequestMapping("test")
    public String test() {
        return null;
    }

    @RequestMapping("table/{id}/")
    public Map deviceTable(@PathVariable("id")String id, @PathVariable("id1")String id1, MainActivity activity) {
        System.out.println(id);
        System.out.println(id1);
        System.out.println(activity);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("11", "22");
        map.put("33", "44");
        return map;
    }

    @RequestMapping("info")
    public void info(@RequestParam("deviceID") String deviceID) {
    }


    @RequestMapping("info3")
    public void info2(@PathVariable("deviceID") String deviceID, @RequestHeader Map<String, String> headers) {
    }

    @RequestMapping("info1")
    public void info1(AsyncTask<Void, String, VerifyError> verifyErrorAsyncTask) {
    }
}
