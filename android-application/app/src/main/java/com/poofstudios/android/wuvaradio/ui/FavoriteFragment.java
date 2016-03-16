package com.poofstudios.android.wuvaradio.ui;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.model.Favorite;

public class FavoriteFragment extends MediaBaseFragment {

    private RecyclerView mRecyclerView;
    private FavoriteAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private CoordinatorLayout mCoordinatorLayout;

    private ItemTouchHelper.SimpleCallback mSwipeItemTouchCallback = new ItemTouchHelper
            .SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            // Get the favorite to be removed
            final Favorite favorite = mAdapter.getFavorite(position);

            // Remove the favorite and possibly update the current song metadata
            boolean songWasUpdated = maybeUpdateCurrentSongFavorite(favorite);
            if (!songWasUpdated) {
                // Remove the favorite from the adapter if the session did not handle it
                mAdapter.removeFavorite(position);
            }

            // Make a snackbar to show the user
            Snackbar snackbar = Snackbar.make(mCoordinatorLayout, "Removed song from favorites", Snackbar.LENGTH_LONG);

            // Set the undo action for the snackbar
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Add the favorite back to the user's favorites
                    // Possibly update the current song metadata
                    boolean songWasUpdated = maybeUpdateCurrentSongFavorite(favorite);

                    if (!songWasUpdated) {
                        // Add the favorite from the adapter if the session did not handle it
                        mAdapter.addFavorite(favorite);
                    }
                }
            });

            // Make the undo button yellow
            snackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.lightColorPrimary));
            snackbar.show();
        }
    };

    public FavoriteFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorite, container, false);

        mCoordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinator_layout);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LineDividerDecoration(getActivity(), getResources()));

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSwipeItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mAdapter = new FavoriteAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    protected void updatePlaybackState(PlaybackStateCompat playbackState) {
        // Playback state does not affect recently played
    }

    @Override
    protected void updateMediaDescription(MediaDescriptionCompat description) {
        // Refresh the data in the adapter
        mAdapter.updateData();
    }
}