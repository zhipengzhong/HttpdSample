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

        try {
            new HttpServer().start();
        } catch (IOException e) {
        }
    }
}
