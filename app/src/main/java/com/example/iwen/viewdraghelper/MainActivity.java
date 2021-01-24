package com.example.iwen.viewdraghelper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.iwen.viewdraghelper.widget.SlideLockView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button goScreenLockBtn = findViewById(R.id.go_screen_lock);
        goScreenLockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScreenLockActivity.class));
                finish();
            }
        });
    }
}