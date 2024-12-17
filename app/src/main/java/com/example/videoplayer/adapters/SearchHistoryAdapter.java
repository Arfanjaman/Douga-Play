package com.example.videoplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.videoplayer.R;
import com.example.videoplayer.models.SearchHistoryItem;

import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private Context context;
    private List<SearchHistoryItem> searchHistoryItems;

    public SearchHistoryAdapter(Context context, List<SearchHistoryItem> searchHistoryItems) {
        this.context = context;
        this.searchHistoryItems = searchHistoryItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchHistoryItem item = searchHistoryItems.get(position);
        holder.videoTitleTextView.setText(item.getTitle());
        Glide.with(context).load(item.getThumbnailUrl()).into(holder.videoThumbnailImageView);

        View.OnClickListener clickListener = v -> {
            String videoUrl = "https://www.youtube.com/watch?v=" + item.getVideoId();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
            context.startActivity(intent);
        };

        holder.videoTitleTextView.setOnClickListener(clickListener);
        holder.videoThumbnailImageView.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return searchHistoryItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView videoTitleTextView;
        ImageView videoThumbnailImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            videoTitleTextView = itemView.findViewById(R.id.video_title_text_view);
            videoThumbnailImageView = itemView.findViewById(R.id.video_thumbnail_image_view);
        }
    }
}