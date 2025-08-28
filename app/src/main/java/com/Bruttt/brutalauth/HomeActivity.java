package com.Bruttt.brutalauth;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomeActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0E0E12"));

        TextView ok = new TextView(this);
        ok.setText("✅ Login Successful\nThis is your Home page.");
        ok.setTextColor(Color.WHITE);
        ok.setTextSize(18);
        ok.setGravity(Gravity.CENTER);
        root.addView(ok);

        setContentView(root);
    }
}
