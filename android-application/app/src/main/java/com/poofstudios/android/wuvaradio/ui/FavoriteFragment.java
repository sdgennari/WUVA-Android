package com.poofstudios.android.wuvaradio.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.model.Favorite;

public class FavoriteFragment extends MediaBaseFragment {

    public interface OnRadioButtonPressedListener {
        void onRadioButtonPressed();
    }
    private OnRadioButtonPressedListener mOnRadioButtonPressedListener;

    private RecyclerView mRecyclerView;
    private FavoriteAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private CoordinatorLayout mCoordinatorLayout;
    private ViewGroup mErrorLayout;

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

                    // Update the empty state since the song was re-added
                    updateEmptyState();
                }
            });

            // Make the undo button yellow
            snackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.lightColorPrimary));
            snackbar.show();

            // Update the empty state
            updateEmptyState();
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
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mOnRadioButtonPressedListener = (OnRadioButtonPressedListener) context;
        } catch (ClassCastException e) {
            Log.e("WUVA", "Context must implement TuneInButtonListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorite, container, false);

        mCoordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinator_layout);

        // Set up the error layout
        mErrorLayout = (ViewGroup) rootView.findViewById(R.id.error_layout);

        TextView errorTitleView = (TextView) mErrorLayout.findViewById(R.id.error_title);
        errorTitleView.setText(getString(R.string.favorite_empty_title));

        TextView errorMessageView = (TextView) mErrorLayout.findViewById(R.id.error_message);
        errorMessageView.setText(getString(R.string.favorite_empty_message));

        Button radioButton = (Button) mErrorLayout.findViewById(R.id.error_action);
        radioButton.setText(getString(R.string.favorite_empty_action_radio));
        radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnRadioButtonPressedListener != null) {
                    mOnRadioButtonPressedListener.onRadioButtonPressed();
                }
            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LineDividerDecoration(getActivity(), getResources()));

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSwipeItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mAdapter = new FavoriteAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        // Show the empty state if no favorites
        updateEmptyState();

        return rootView;
    }

    private void updateEmptyState() {
        if (mAdapter.getItemCount() <= 0) {
            mErrorLayout.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mErrorLayout.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void updatePlaybackState(PlaybackStateCompat playbackState) {
        // Playback state does not affect recently played
    }

    @Override
    protected void updateMediaDescription(MediaDescriptionCompat description) {
        // Refresh the data in the adapter
        mAdapter.updateData();

        // Update the empty state
        updateEmptyState();
    }
}