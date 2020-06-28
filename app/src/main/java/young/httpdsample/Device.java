package young.httpdsample;

import java.util.Map;

import young.httpd.annotation.RequestMapping;
import young.httpd.annotation.RequestParam;

@RequestMapping("/admin/v1/api/device/")
public class Device {

    private static final String DEVICE = "device";

    @RequestMapping("/table")
    public Map deviceTable(String abcd, Map<String, String> map) {
        return null;
    }

    @RequestMapping("/table1/")
    private Map deviceTable1() {
        return null;
    }

    @RequestMapping("info/")
    public void info(@RequestParam("deviceID") String deviceID) {
    }

}
