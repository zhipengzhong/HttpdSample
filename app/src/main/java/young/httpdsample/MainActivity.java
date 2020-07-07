package young.httpdsample;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import young.httpd.HttpServer;

public class MainActivity extends AppCompatActivity {

    private Long mTimeMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HttpServer httpServer = new HttpServer();
        httpServer.inject(this);
        try {
            httpServer.start();
        } catch (IOException e) {
        }

        mTimeMillis = System.currentTimeMillis();
        httpServer.inject(mTimeMillis);

        // 10秒后去除强引用 并代码触发GC
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mTimeMillis = null;
                System.gc();
                Toast.makeText(MainActivity.this, "去除mTimeMillis强引用", Toast.LENGTH_SHORT).show();
            }
        }, 10000);
    }
}
