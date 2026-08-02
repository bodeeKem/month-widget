package com.kemd.monthwidget;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText("Calendar permission granted. Now long-press your home screen, choose Widgets, and add 'Month Widget'.");
        tv.setPadding(40, 100, 40, 40);
        setContentView(tv);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR}, 1);
        }
    }
}
