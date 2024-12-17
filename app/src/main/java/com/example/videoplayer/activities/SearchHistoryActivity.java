package com.example.videoplayer.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.videoplayer.R;
import com.example.videoplayer.adapters.SearchHistoryAdapter;
import com.example.videoplayer.models.SearchHistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchHistoryActivity extends AppCompatActivity {

    private RecyclerView searchHistoryRecyclerView;
    private SearchHistoryAdapter searchHistoryAdapter;
    private List<SearchHistoryItem> searchHistoryItems;
    private Button clearHistoryButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_history);

        searchHistoryRecyclerView = findViewById(R.id.search_history_recycler_view);
        searchHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchHistoryItems = new ArrayList<>();
        searchHistoryAdapter = new SearchHistoryAdapter(this, searchHistoryItems);
        searchHistoryRecyclerView.setAdapter(searchHistoryAdapter);

        clearHistoryButton = findViewById(R.id.clear_history_button);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        fetchSearchHistory();

        clearHistoryButton.setOnClickListener(v -> clearSearchHistory());
    }

    private void fetchSearchHistory() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            mDatabase.child("SearchesPerUser").child(userId).child("searches").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Set<String> uniqueUrls = new HashSet<>();
                    searchHistoryItems.clear();
                    for (DataSnapshot searchSnapshot : snapshot.getChildren()) {
                        String url = searchSnapshot.child("url").getValue(String.class);
                        long timestamp = searchSnapshot.child("timestamp").getValue(Long.class);
                        if (uniqueUrls.add(url)) {
                            fetchVideoDetails(url, timestamp);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("SearchHistoryActivity", "Failed to fetch search history", error.toException());
                }
            });
        }
    }

    private void fetchVideoDetails(String url, long timestamp) {
        String videoId = extractVideoIdFromUrl(url);
        if (videoId != null) {
            String apiUrl = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId + "&key=AIzaSyC1bO_LlQ-pdxHdsXQCR0YOMC4gKa-yt4U&part=snippet";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, apiUrl, null,
                    response -> {
                        try {
                            JSONObject video = response.getJSONArray("items").getJSONObject(0).getJSONObject("snippet");
                            String title = video.getString("title");
                            String thumbnailUrl = video.getJSONObject("thumbnails").getJSONObject("high").getString("url");
                            searchHistoryItems.add(new SearchHistoryItem(title, thumbnailUrl, timestamp, videoId));
                            Collections.sort(searchHistoryItems, (item1, item2) -> Long.compare(item2.getTimestamp(), item1.getTimestamp()));
                            searchHistoryAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }, error -> {
                error.printStackTrace();
            });

            Volley.newRequestQueue(this).add(request);
        }
    }

    private String extractVideoIdFromUrl(String url) {
        String videoId = null;
        if (url.contains("v=")) {
            videoId = url.split("v=")[1];
            int ampersandPosition = videoId.indexOf('&');
            if (ampersandPosition != -1) {
                videoId = videoId.substring(0, ampersandPosition);
            }
        } else if (url.contains("youtu.be/")) {
            videoId = url.split("youtu.be/")[1];
            int questionMarkPosition = videoId.indexOf('?');
            if (questionMarkPosition != -1) {
                videoId = videoId.substring(0, questionMarkPosition);
            }
        }
        return videoId;
    }

    private void clearSearchHistory() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            mDatabase.child("SearchesPerUser").child(userId).child("searches").removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            searchHistoryItems.clear();
                            searchHistoryAdapter.notifyDataSetChanged();
                            Toast.makeText(SearchHistoryActivity.this, "Search history cleared successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SearchHistoryActivity.this, "Failed to clear search history", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}