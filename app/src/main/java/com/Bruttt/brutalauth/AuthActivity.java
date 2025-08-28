package com.Bruttt.brutalauth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AuthActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String username = getIntent().getStringExtra("username");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.parseColor("#121319"));

        TextView title = new TextView(this);
        title.setText("Authentication");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Welcome, " + (username == null ? "user" : username) + "\nYou're authenticated.");
        info.setTextSize(14);
        info.setTextColor(0xFFBBBBBB);
        info.setGravity(Gravity.CENTER);

        Button go = new Button(this);
        go.setText("Continue to Home");
        go.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        root.addView(title);
        root.addView(info);
        root.addView(go);
        setContentView(root);
    }
}
