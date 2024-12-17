package com.example.videoplayer.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import com.example.videoplayer.R;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(SplashScreenActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted, navigate to the main activity
                    startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
                } else {
                    // Permission is not granted, navigate to AllowAccessActivity
                    startActivity(new Intent(SplashScreenActivity.this, AllowAccessActivity.class));
                }
                finish();
            }
        }, 3000);
    }
}