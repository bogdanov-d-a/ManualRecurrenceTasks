package ru.trjoxuvw.manualrecurrencetasks;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import notification.NotificationUtils;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        final Button regButton = (Button) findViewById(R.id.regButton);
        assert regButton != null;
        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationUtils.registerAllGroupsWithData(DebugActivity.this);
            }
        });

        final Button unregButton = (Button) findViewById(R.id.unregButton);
        assert unregButton != null;
        unregButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationUtils.unregisterAllGroupsWithData(DebugActivity.this);
            }
        });
    }
}
