package young.httpdsample;

import java.util.Map;

import young.httpd.annotation.RequestMapping;
import young.httpd.annotation.RequestParam;
@RequestMapping()
public class Test1 {

//    @RequestMapping("test")
    public String test() {
        return null;
    }

//    @RequestMapping("table")
    public Map deviceTable() {
        return null;
    }

//    @RequestMapping("info")
    public void info(@RequestParam("deviceID") String deviceID) {
    }

}
