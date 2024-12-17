package com.example.videoplayer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.videoplayer.R;

import org.json.JSONException;
import org.json.JSONObject;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class YoutubeActivity extends AppCompatActivity {

    private EditText urlEditText;
    private Button searchButton;
    private TextView videoTitleTextView;
    private ImageView videoThumbnailImageView;
    private WebView youtubeWebView;
    private RequestQueue requestQueue;
    private String apiKey = "AIzaSyC1bO_LlQ-pdxHdsXQCR0YOMC4gKa-yt4U"; // Api key goes here
    private String videoId;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube);
        Button searchHistoryButton = findViewById(R.id.search_history_button);
        urlEditText = findViewById(R.id.url_edit_text);
        searchButton = findViewById(R.id.search_button);
        videoTitleTextView = findViewById(R.id.video_title_text_view);
        videoThumbnailImageView = findViewById(R.id.video_thumbnail_image_view);
        youtubeWebView = findViewById(R.id.youtube_webview);

        requestQueue = Volley.newRequestQueue(this);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        searchButton.setOnClickListener(v -> {
            String url = urlEditText.getText().toString().trim();
            if (!url.isEmpty()) {
                fetchYoutubeVideo(url);
                storeSearchData(url);
            } else {
                Toast.makeText(YoutubeActivity.this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            }
        });

        videoThumbnailImageView.setOnClickListener(v -> {
            if (videoId != null) {
                playYoutubeVideo(videoId);
            }
        });
        searchHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(YoutubeActivity.this, SearchHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void fetchYoutubeVideo(String url) {
        videoId = extractVideoIdFromUrl(url);
        if (videoId != null) {
            String apiUrl = "https://www.googleapis.com/youtube/v3/videos?id=" + videoId + "&key=" + apiKey + "&part=snippet";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, apiUrl, null,
                    response -> {
                        try {
                            JSONObject video = response.getJSONArray("items").getJSONObject(0).getJSONObject("snippet");
                            String title = video.getString("title");
                            String thumbnailUrl = video.getJSONObject("thumbnails").getJSONObject("high").getString("url");
                            videoTitleTextView.setText(title);
                            Glide.with(YoutubeActivity.this).load(thumbnailUrl).into(videoThumbnailImageView);
                            playVideoInWebView(videoId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(YoutubeActivity.this, "Failed to parse video data", Toast.LENGTH_SHORT).show();
                        }
                    }, error -> {
                error.printStackTrace();
                Toast.makeText(YoutubeActivity.this, "Failed to fetch video data", Toast.LENGTH_SHORT).show();
            });

            requestQueue.add(request);
        } else {
            Toast.makeText(this, "Invalid YouTube URL", Toast.LENGTH_SHORT).show();
        }
    }

    private void storeSearchData(String url) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            String searchId = mDatabase.child("SearchesPerUser").child(userId).child("searches").push().getKey();
            if (searchId != null) {
                Map<String, Object> searchData = new HashMap<>();
                searchData.put("url", url);
                searchData.put("timestamp", System.currentTimeMillis());

                mDatabase.child("SearchesPerUser").child(userId).child("searches").child(searchId).setValue(searchData)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(YoutubeActivity.this, "Search data stored successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(YoutubeActivity.this, "Failed to store search data", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private void playVideoInWebView(String videoId) {
        String videoUrl = "https://www.youtube.com/embed/" + videoId;
        String videoHtml = "<iframe width=\"100%\" height=\"100%\" src=\"" + videoUrl + "\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>";
        youtubeWebView.loadData(videoHtml, "text/html", "utf-8");
        WebSettings webSettings = youtubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        youtubeWebView.setWebChromeClient(new WebChromeClient());
    }

    private void playYoutubeVideo(String videoId) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + videoId));
        startActivity(intent);
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
}