package com.poofstudios.android.wuvaradio.ui;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.poofstudios.android.wuvaradio.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<MediaDescriptionCompat> mCuePointDescriptionList;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitleView;
        public TextView mArtistView;
        public ImageView mCoverArtView;

        public ViewHolder(View root) {
            super(root);

            mTitleView = (TextView) root.findViewById(R.id.title);
            mArtistView = (TextView) root.findViewById(R.id.artist);
            mCoverArtView = (ImageView) root.findViewById(R.id.image);
        }
    }

    public HistoryAdapter() {
        this.mCuePointDescriptionList = new ArrayList<>();
    }

    public void setData(List<MediaDescriptionCompat> data) {
        this.mCuePointDescriptionList = data;
    }

    // Creates a new view
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate view and create new ViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recently_played, parent, false);
        return new ViewHolder(view);
    }

    // Replace the contents of the list item
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MediaDescriptionCompat description = mCuePointDescriptionList.get(position);

        holder.mTitleView.setText(description.getTitle());
        holder.mArtistView.setText(description.getSubtitle());

        // Load cover art image if available
        if (description.getIconUri() != null) {
            Picasso.with(holder.mCoverArtView.getContext())
                    .load(description.getIconUri().toString())
                    .placeholder(R.drawable.cover_art_placeholder)
                    .error(R.drawable.cover_art_placeholder)
                    .into(holder.mCoverArtView);
        } else {
            Picasso.with(holder.mCoverArtView.getContext())
                    .load(R.drawable.cover_art_placeholder)
                    .into(holder.mCoverArtView);
        }
    }

    @Override
    public int getItemCount() {
        return mCuePointDescriptionList.size();
    }
}
