package com.cscd488_490.team5.i_nav;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DirectionsActivity extends AppCompatActivity {

    private LocationMap locationMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions);

        Intent intent = getIntent();
        if (intent != null) {
            String directions = intent.getStringExtra("directions");
            TextView tview = (TextView) findViewById(R.id.directions_text);
            tview.setText(directions);
        }
    }
}
