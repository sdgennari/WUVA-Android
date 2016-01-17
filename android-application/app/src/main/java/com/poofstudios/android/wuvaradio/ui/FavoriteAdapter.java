package com.poofstudios.android.wuvaradio.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.poofstudios.android.wuvaradio.FavoriteManager;
import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.model.Favorite;
import com.poofstudios.android.wuvaradio.model.Track;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    private FavoriteManager mFavoriteManager;
    private List<Favorite> mFavoriteList;

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

    public FavoriteAdapter(Context context) {
        mFavoriteManager = FavoriteManager.getFavoriteManager(context);

        // Set the current favorites as the data for the adapter
        mFavoriteList = new ArrayList<>(mFavoriteManager.getFavorites());
        sortFavoritesByTrack();
    }

    public void updateData() {
        // Get the new list of favorites
        HashSet<Favorite> favorites = mFavoriteManager.getFavorites();

        // Only update the adapter if a favorite was added/removed
        if (favorites.size() != mFavoriteList.size()) {
            mFavoriteList = new ArrayList<>(favorites);
            sortFavoritesByTrack();

            // Notify the adapter that the data has changed
            notifyDataSetChanged();
        }
    }

    public Favorite removeFavorite(int position) {
        // Get the favorite from the list
        Favorite favorite = mFavoriteList.remove(position);

        // Remove the song from the user's favorites
        mFavoriteManager.setFavorite(favorite, false);

        // Notify the adapter that a favorite has been removed
        notifyItemRemoved(position);

        return favorite;
    }

    public void addFavorite(Favorite favorite) {
        // Add the favorite to the adapter
        mFavoriteList.add(favorite);
        sortFavoritesByTrack();

        // Add the song to the user's favorites
        mFavoriteManager.setFavorite(favorite, true);

        // Notify the adapter that a favorite has been added
        notifyItemInserted(mFavoriteList.indexOf(favorite));
    }

    /**
     * Sorts favorites by when they were created
     */
    private void sortFavoritesByTrack() {
        Collections.sort(mFavoriteList, new Comparator<Favorite>() {
            @Override
            public int compare(Favorite lhs, Favorite rhs) {
                Track lhsTrack = lhs.getTrack();
                Track rhsTrack = rhs.getTrack();
                return lhsTrack.getTitle().compareTo(rhsTrack.getTitle());
            }
        });
    }

    // Creates a new view
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate view and create new ViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    // Replaces the contents of the list item
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Favorite favorite = mFavoriteList.get(position);
        Track track = favorite.getTrack();

        holder.mTitleView.setText(track.getTitle());
        holder.mArtistView.setText(track.getArtist());

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
        return mFavoriteList.size();
    }
}
