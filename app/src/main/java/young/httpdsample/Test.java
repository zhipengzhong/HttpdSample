package young.httpdsample;

import android.os.AsyncTask;

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

    @RequestMapping("table")
    public Map deviceTable() {
        return null;
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
