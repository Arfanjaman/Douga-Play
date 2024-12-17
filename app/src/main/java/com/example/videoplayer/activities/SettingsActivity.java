package com.example.videoplayer.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import com.example.videoplayer.R;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchDarkMode;
    private Switch switchStoragePermission;
    private Switch switchCameraPermission;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editor = sharedPreferences.edit();

        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchStoragePermission = findViewById(R.id.switch_storage_permission);
        switchCameraPermission = findViewById(R.id.switch_camera_permission);

        // Loading saved preferences
        loadPreferences();

        // Dark Mode Toggle
        switchDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                editor.putBoolean("DarkMode", isChecked);
                editor.apply();
                 //Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                   // startActivity(intent);


            }
        });

        switchCameraPermission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(SettingsActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // Open device settings to enable camera permission
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    } else {
                        editor.putBoolean("CameraPermission", true);
                        editor.apply();
                    }
                } else {
                    editor.putBoolean("CameraPermission", false);
                    editor.apply();
                }
            }
        });
        //no use
        switchStoragePermission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // Open device settings to enable storage permission
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    } else {
                        editor.putBoolean("StoragePermission", true);
                        editor.apply();
                    }
                } else {
                    editor.putBoolean("StoragePermission", false);
                    editor.apply();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            switchStoragePermission.setChecked(true);
            editor.putBoolean("StoragePermission", true);
            editor.apply();
        }
    }

    private void loadPreferences() {
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        boolean isStoragePermissionGranted = sharedPreferences.getBoolean("StoragePermission", false);
        boolean isCameraPermissionGranted = sharedPreferences.getBoolean("CameraPermission", false);

        switchDarkMode.setChecked(isDarkMode);
        switchStoragePermission.setChecked(isStoragePermissionGranted);
        switchCameraPermission.setChecked(isCameraPermissionGranted);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (requestCode == 1) {
            editor.putBoolean("StoragePermission", isPermissionGranted);
        } else if (requestCode == 2) {
            editor.putBoolean("CameraPermission", isPermissionGranted);
        }
        editor.apply();

        // Pass updated permissions back to MainActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("StoragePermission", sharedPreferences.getBoolean("StoragePermission", false));
        resultIntent.putExtra("CameraPermission", sharedPreferences.getBoolean("CameraPermission", false));
        setResult(RESULT_OK, resultIntent);
    }
}