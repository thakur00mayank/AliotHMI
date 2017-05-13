package io.datonis.aliothmi;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * Created by mayank on 7/5/17.
 */

public class NotificationHandlerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stopService(new Intent(this, MyService.class));
        finishAffinity();
    }
}
