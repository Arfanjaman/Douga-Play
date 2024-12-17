package com.example.videoplayer.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.videoplayer.R;
import com.example.videoplayer.adapters.VideoFilesAdapter;
import com.example.videoplayer.models.MediaFiles;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;

public class RecentVideosActivity extends AppCompatActivity {

    private static final String TAG = "RecentVideosActivity";
    private RecyclerView recyclerView;
    private VideoFilesAdapter videoFilesAdapter;
    private ArrayList<MediaFiles> videoFilesArrayList = new ArrayList<>();
    private HashSet<String> uniquePaths = new HashSet<>();
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_videos);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_recent_videos);
        recyclerView = findViewById(R.id.recent_videos_rv);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setNestedScrollingEnabled(true);

        videoFilesAdapter = new VideoFilesAdapter(videoFilesArrayList, this, 0,false);
        recyclerView.setAdapter(videoFilesAdapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentUser != null) {
                fetchRecentVideos();
            } else {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            }
        });

        if (currentUser != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("PlayedVideos").child(currentUser.getUid());
            fetchRecentVideos();
        } else {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchRecentVideos() {
        swipeRefreshLayout.setRefreshing(true);
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                videoFilesArrayList.clear();
                uniquePaths.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        VideoDetails videoDetails = snapshot.getValue(VideoDetails.class);
                        if (videoDetails != null && videoDetails.title != null && videoDetails.path != null) {
                            if (!uniquePaths.contains(videoDetails.path)) {
                                MediaFiles mediaFiles = fetchMediaFile(videoDetails.path);
                                if (mediaFiles != null) {
                                    videoFilesArrayList.add(mediaFiles);
                                    uniquePaths.add(videoDetails.path);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing snapshot: " + e.getMessage());
                    }
                }
                videoFilesAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch data: " + databaseError.getMessage());
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(RecentVideosActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
            }
        };
        databaseReference.addListenerForSingleValueEvent(valueEventListener);
    }

    private MediaFiles fetchMediaFile(String path) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Video.Media.DATA + "=?";
        String[] selectionArgs = new String[]{path};
        Cursor cursor = getContentResolver().query(uri, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
            @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
            @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
            @SuppressLint("Range") String size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
            @SuppressLint("Range") String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
            @SuppressLint("Range") String dateAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));
            cursor.close();
            return new MediaFiles(id, title, displayName, size, duration, path, dateAdded);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseReference != null && valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }

    public static class VideoDetails {
        public String path;
        public String title;

        public VideoDetails() {
            // Default constructor required for calls to DataSnapshot.getValue(VideoDetails.class)
        }

        public VideoDetails(String path, String title) {
            this.path = path;
            this.title = title;
        }
    }
}