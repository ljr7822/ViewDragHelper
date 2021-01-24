package com.example.iwen.viewdraghelper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.iwen.viewdraghelper.widget.SlideLockView;

/**
 * 锁屏页面
 */
public class ScreenLockActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_lock);
        SlideLockView slideRail = findViewById(R.id.slide_rail);
        slideRail.setCallback(new SlideLockView.Callback() {
            @Override
            public void onUnlock() {
                //解锁，跳转到首页
                Intent intent = new Intent(ScreenLockActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}