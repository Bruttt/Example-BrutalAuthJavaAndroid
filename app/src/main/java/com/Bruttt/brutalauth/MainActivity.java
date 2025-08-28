package com.Bruttt.brutalauth;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Build the login/register UI entirely in code
        new Login(this);
    }
}
