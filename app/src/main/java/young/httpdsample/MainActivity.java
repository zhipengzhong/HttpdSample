package young.httpdsample;

import androidx.appcompat.app.AppCompatActivity;
import young.httpd.HttpServer;

import android.os.Bundle;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HttpServer httpServer = new HttpServer();
        httpServer.addInject(this);
        try {
            httpServer.start();
        } catch (IOException e) {
        }
    }
}
