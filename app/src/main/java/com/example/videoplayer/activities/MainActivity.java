package com.example.videoplayer.activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.videoplayer.models.MediaFiles;
import com.example.videoplayer.R;
import com.example.videoplayer.adapters.VideoFoldersAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<MediaFiles> mediaFiles = new ArrayList<>();
    private ArrayList<String> allFolderList = new ArrayList<>();
    RecyclerView recyclerView;
    VideoFoldersAdapter adapter;
    SwipeRefreshLayout swipeRefreshLayout;
    private boolean isCameraPermissionGranted;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.folders_rv);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_folders);
        showFolders();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showFolders();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_profile:
                        startActivity(new Intent(MainActivity.this, AccountActivity.class));
                        return true;
                    case R.id.nav_recent:
                        startActivity(new Intent(MainActivity.this, RecentVideosActivity.class));
                        return true;
                    case R.id.nav_settings:
                        startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 1);
                        return true;
                    case R.id.nav_record:
                        if (isCameraPermissionGranted) {
                            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "Camera permission is required to record video", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    case R.id.nav_yt:
                        startActivity(new Intent(MainActivity.this, YoutubeActivity.class));
                        return true;
                }
                return false;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            finish();
            startActivity(getIntent());
        }

        if (requestCode == 1 && resultCode == RESULT_OK) {


            isCameraPermissionGranted = data.getBooleanExtra("CameraPermission", false);

            // Disable Record functionality if camera permission is off
            if (!isCameraPermissionGranted) {
                Toast.makeText(this, "Camera permission is disabled. Record function will not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }


        isCameraPermissionGranted = sharedPreferences.getBoolean("CameraPermission", false);

    }

    private void showFolders() {
        mediaFiles = fetchMedia();
        adapter = new VideoFoldersAdapter(mediaFiles, allFolderList, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                RecyclerView.VERTICAL, false));
        adapter.notifyDataSetChanged();
    }

    public ArrayList<MediaFiles> fetchMedia() {
        ArrayList<MediaFiles> mediaFilesArrayList = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = getContentResolver().query(uri, null,
                null, null, null);
        if (cursor != null && cursor.moveToNext()) {
            do {
                @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                @SuppressLint("Range") String size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                @SuppressLint("Range") String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                @SuppressLint("Range") String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                @SuppressLint("Range") String dateAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));
                MediaFiles mediaFiles = new MediaFiles(id, title, displayName, size, duration, path,
                        dateAdded);

                int index = path.lastIndexOf("/");
                String subString = path.substring(0, index);
                if (!allFolderList.contains(subString)) {
                    allFolderList.add(subString);
                }
                mediaFilesArrayList.add(mediaFiles);
            } while (cursor.moveToNext());
        }
        return mediaFilesArrayList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.folder_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.fav:
                startActivity(new Intent(MainActivity.this, FavouriteVideosActivity.class));
                return true;
            case R.id.refresh_folders:
                finish();
                startActivity(getIntent());
                break;
            case R.id.share_app:
                Intent share_intent = new Intent();
                share_intent.setAction(Intent.ACTION_SEND);
                share_intent.putExtra(Intent.EXTRA_TEXT, "Check this app via\n" +
                        "https://drive.google.com/drive/folders/1UZl64wqD5Thx1AW5izvtFKjbmD8EN75v?usp=sharing"
                        + getApplicationContext().getPackageName());
                share_intent.setType("text/plain");
                startActivity(Intent.createChooser(share_intent, "Share app via"));
                break;
        }
        return true;
    }
}