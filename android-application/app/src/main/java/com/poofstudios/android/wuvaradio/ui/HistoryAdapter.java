package com.poofstudios.android.wuvaradio.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.poofstudios.android.wuvaradio.FavoriteManager;
import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.model.Favorite;
import com.poofstudios.android.wuvaradio.model.Track;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private FavoriteManager mFavoriteManager;
    private List<Track> mCuePointDescriptionList;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitleView;
        public TextView mArtistView;
        public ImageView mCoverArtView;
        public ToggleButton mFavoriteToggle;

        public ViewHolder(View root) {
            super(root);

            mTitleView = (TextView) root.findViewById(R.id.title);
            mArtistView = (TextView) root.findViewById(R.id.artist);
            mCoverArtView = (ImageView) root.findViewById(R.id.image);
            mFavoriteToggle = (ToggleButton) root.findViewById(R.id.toggle_favorite);
        }
    }

    public HistoryAdapter(Context context) {
        this.mCuePointDescriptionList = new ArrayList<>();
        mFavoriteManager = FavoriteManager.getFavoriteManager(context);
    }

    public void setData(List<Track> data) {
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
        // Increment position by 1 to skip the first item in the list
        Track track = mCuePointDescriptionList.get(position+1);
        final Favorite favorite = new Favorite(track);

        holder.mTitleView.setText(track.getTitle());
        holder.mArtistView.setText(track.getArtist());
        holder.mFavoriteToggle.setOnCheckedChangeListener(null);
        holder.mFavoriteToggle.setChecked(mFavoriteManager.isFavorite(favorite));

        holder.mFavoriteToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // isChecked is true when the song should be added to favorites
                mFavoriteManager.setFavorite(favorite, isChecked);
            }
        });

        // Load cover art image if available
        if (track.getCoverArtUrl() != null) {
            Picasso.with(holder.mCoverArtView.getContext())
                    .load(track.getCoverArtUrl())
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
        // Decrement size by 1 since the first element is not shown
        return mCuePointDescriptionList.size()-1;
    }
}
