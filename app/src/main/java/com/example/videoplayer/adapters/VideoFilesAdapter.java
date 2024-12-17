package com.example.videoplayer.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.videoplayer.models.MediaFiles;
import com.example.videoplayer.R;
import com.example.videoplayer.models.Utility;
import com.example.videoplayer.activities.VideoPlayerActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.util.ArrayList;

public class VideoFilesAdapter extends RecyclerView.Adapter<VideoFilesAdapter.ViewHolder> {
    private ArrayList<MediaFiles> videoList;
    private Context context;
    BottomSheetDialog bottomSheetDialog;
    private int viewType;
    private boolean isFavouriteActivity;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;

    // Constructor with three parameters
    public VideoFilesAdapter(ArrayList<MediaFiles> videoList, Context context, int viewType) {
        this(videoList, context, viewType, false);
    }

    // Constructor with four parameters
    public VideoFilesAdapter(ArrayList<MediaFiles> videoList, Context context, int viewType, boolean isFavouriteActivity) {
        this.videoList = videoList;
        this.context = context;
        this.viewType = viewType;
        this.isFavouriteActivity = isFavouriteActivity;
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("FavouriteVideos").child(currentUser.getUid());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoFilesAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.videoName.setText(videoList.get(position).getDisplayName());
        String size = videoList.get(position).getSize();
        holder.videoSize.setText(android.text.format.Formatter.formatFileSize(context, Long.parseLong(size)));
        double milliSeconds = Double.parseDouble(videoList.get(position).getDuration());
        holder.videoDuration.setText(Utility.timeConversion((long) milliSeconds));

        Glide.with(context).load(new File(videoList.get(position).getPath())).into(holder.thumbnail);

        if (viewType == 0) {
            holder.menu_more.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("MissingInflatedId")
                @Override
                public void onClick(View v) {
                    bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);
                    View bsView = LayoutInflater.from(context).inflate(R.layout.video_bs_layout, v.findViewById(R.id.bottom_sheet));
                    bsView.findViewById(R.id.bs_play).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.itemView.performClick();
                            bottomSheetDialog.dismiss();
                        }
                    });
                    bsView.findViewById(R.id.bs_rename).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                            alertDialog.setTitle("Rename to");
                            EditText editText = new EditText(context);
                            String path = videoList.get(position).getPath();
                            final File file = new File(path);
                            String videoName = file.getName();
                            //removing the extension
                            videoName = videoName.substring(0, videoName.lastIndexOf("."));
                            editText.setText(videoName);
                            alertDialog.setView(editText);
                            editText.requestFocus();

                            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (TextUtils.isEmpty(editText.getText().toString())) {
                                        Toast.makeText(context, "Can't rename empty file", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    String onlyPath = file.getParentFile().getAbsolutePath();
                                    String ext = file.getAbsolutePath();
                                    ext = ext.substring(ext.lastIndexOf("."));
                                    String newPath = onlyPath + "/" + editText.getText().toString() + ext;
                                    File newFile = new File(newPath);
                                    boolean rename = file.renameTo(newFile);
                                    if (rename) {
                                        ContentResolver resolver = context.getApplicationContext().getContentResolver();
                                        resolver.delete(MediaStore.Files.getContentUri("external"), MediaStore.MediaColumns.DATA + "=?", new String[]{file.getAbsolutePath()});
                                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                        intent.setData(Uri.fromFile(newFile));
                                        context.getApplicationContext().sendBroadcast(intent);

                                        notifyDataSetChanged();
                                        Toast.makeText(context, "Video Renamed", Toast.LENGTH_SHORT).show();

                                        SystemClock.sleep(200);
                                        ((Activity) context).recreate();
                                    } else {
                                        Toast.makeText(context, "Process Failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            alertDialog.create().show();
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bsView.findViewById(R.id.bs_share).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Uri uri = Uri.parse(videoList.get(position).getPath());
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("video/*");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video via"));
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bsView.findViewById(R.id.bs_delete).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                            alertDialog.setTitle("Delete");
                            alertDialog.setMessage("Do you want to delete this video");
                            alertDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Long.parseLong(videoList.get(position).getId()));
                                    File file = new File(videoList.get(position).getPath());
                                    boolean delete = file.delete();
                                    if (delete) {
                                        context.getContentResolver().delete(contentUri, null, null);
                                        videoList.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, videoList.size());
                                        Toast.makeText(context, "Video Deleted", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "Can't delete", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            alertDialog.show();
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bsView.findViewById(R.id.bs_properties).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                            alertDialog.setTitle("Properties");

                            String one = "File: " + videoList.get(position).getDisplayName();

                            String path = videoList.get(position).getPath();
                            int indexOfPath = path.lastIndexOf("/");
                            String two = "Path: " + path.substring(0, indexOfPath);

                            String three = "Size: " + android.text.format.Formatter.formatFileSize(context, Long.parseLong(videoList.get(position).getSize()));

                            String four = "Length: " + Utility.timeConversion((long) milliSeconds);

                            String namewithFormat = videoList.get(position).getDisplayName();
                            int index = namewithFormat.lastIndexOf(".");
                            String format = namewithFormat.substring(index + 1);
                            String five = "Format: " + format;

                            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                            metadataRetriever.setDataSource(videoList.get(position).getPath());
                            String height = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                            String width = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                            String six = "Resolution: " + width + "x" + height;

                            alertDialog.setMessage(one + "\n\n" + two + "\n\n" + three + "\n\n" + four + "\n\n" + five + "\n\n" + six);
                            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            alertDialog.show();
                            bottomSheetDialog.dismiss();
                        }
                    });

                    if (!isFavouriteActivity) {
                        bsView.findViewById(R.id.bs_favourites).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                addToFavourites(videoList.get(position));
                                bottomSheetDialog.dismiss();
                            }
                        });
                    } else {
                        bsView.findViewById(R.id.bs_favourites).setVisibility(View.GONE);
                    }

                    bottomSheetDialog.setContentView(bsView);
                    bottomSheetDialog.show();
                }
            });
        } else {
            holder.menu_more.setVisibility(View.GONE);
            holder.videoName.setTextColor(Color.WHITE);
            holder.videoSize.setTextColor(Color.WHITE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("position", position);
                intent.putExtra("video_title", videoList.get(position).getDisplayName());
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("videoArrayList", videoList);
                intent.putExtras(bundle);
                context.startActivity(intent);
                if (viewType == 1) {
                    ((Activity) context).finish();
                }
            }
        });
    }

    private void addToFavourites(MediaFiles mediaFiles) {
        if (currentUser != null) {
            VideoDetails videoDetails = new VideoDetails(mediaFiles.getPath(), mediaFiles.getTitle());
            databaseReference.push().setValue(videoDetails)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Added to Favourites", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to add to Favourites", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(context, "User not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail, menu_more;
        TextView videoName, videoSize, videoDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            menu_more = itemView.findViewById(R.id.video_menu_more);
            videoName = itemView.findViewById(R.id.video_name);
            videoSize = itemView.findViewById(R.id.video_size);
            videoDuration = itemView.findViewById(R.id.video_duration);
        }
    }

    public void updateVideoFiles(ArrayList<MediaFiles> files) {
        videoList = new ArrayList<>();
        videoList.addAll(files);
        notifyDataSetChanged();
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