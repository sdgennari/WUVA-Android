package com.poofstudios.android.wuvaradio.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.RadioPlayback;
import com.poofstudios.android.wuvaradio.api.CoverArtUrlCache;
import com.poofstudios.android.wuvaradio.model.Favorite;
import com.poofstudios.android.wuvaradio.model.Track;
import com.poofstudios.android.wuvaradio.utils.StringUtils;
import com.tritondigital.player.CuePoint;
import com.tritondigital.player.CuePointHistory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class RecentlyPlayedFragment extends MediaBaseFragment implements
        CuePointHistory.CuePointHistoryListener,
        SwipeRefreshLayout.OnRefreshListener,
        HistoryAdapter.OnSongFavoriteUpdateListener {

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private HistoryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private CuePointHistory mCuePointHistory;
    private ViewGroup mErrorLayout;

    private List<Track> mCuePointDescriptionList;
    private ArrayDeque<Track> mPendingCoverArtRequests;

    // For getting album cover art
    CoverArtUrlCache mUrlCache;

    public RecentlyPlayedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCuePointDescriptionList = new ArrayList<>();
        mPendingCoverArtRequests = new ArrayDeque<>();

        // Get the instance of the url cache
        mUrlCache = CoverArtUrlCache.getUrlCache();

        mCuePointHistory = new CuePointHistory();
        mCuePointHistory.setListener(this);
        mCuePointHistory.setCueTypeFilter(CuePoint.CUE_TYPE_VALUE_TRACK);
        // Get the 16 most recent songs since the first one is the currently playing song
        mCuePointHistory.setMaxItems(16);
        mCuePointHistory.setMount(RadioPlayback.STATION_MOUNT);

        mCuePointHistory.request();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recently_played, container, false);

        // Set up the error layout
        mErrorLayout = (ViewGroup) rootView.findViewById(R.id.error_layout);

        TextView errorTitleView = (TextView) mErrorLayout.findViewById(R.id.error_title);
        errorTitleView.setText(getString(R.string.recently_played_error_title));

        TextView errorMessageView = (TextView) mErrorLayout.findViewById(R.id.error_message);
        errorMessageView.setText(getString(R.string.recently_played_error_message));

        Button tryAgainButton = (Button) mErrorLayout.findViewById(R.id.error_action);
        tryAgainButton.setText(getString(R.string.recently_played_action_try_again));
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshRecentlyPlayedList();
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setRefreshing(true);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LineDividerDecoration(getActivity(), getResources()));

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HistoryAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }


    @Override
    protected void updatePlaybackState(PlaybackStateCompat playbackState) {
        // Playback state does not affect recently played
    }

    @Override
    protected void updateMediaDescription(MediaDescriptionCompat description) {
        // Add the latest song to the recently played list
        String title = String.valueOf(description.getTitle());
        String artist = String.valueOf(description.getSubtitle());
        Track track = new Track(title, artist);

        // Check if song was already added as the most recent song or the 2nd to most recent song
        // Must check the 2nd most recent song since the CuePointHistory can be delayed
        int idx = mCuePointDescriptionList.indexOf(track);
        if (idx != 0 && idx != 1) {
            mCuePointDescriptionList.add(0, track);

            // Only update that an item was added to the adapter
            mAdapter.notifyItemInserted(0);
        }

        // Skip first song since it is not included in the RecyclerView
        for (int i = 1; i < mCuePointDescriptionList.size(); i++) {
            if (track.equals(mCuePointDescriptionList.get(i))) {
                // Decrement value of i to account for offset in RecyclerView
                mAdapter.notifyItemChanged(i-1);
            }
        }
    }

    @Override
    public void onCuePointHistoryReceived(CuePointHistory cuePointHistory, List<Bundle> data) {
        // Remove all old data
        mCuePointDescriptionList.clear();
        mPendingCoverArtRequests.clear();

        // Add each new cue point
        for (Bundle cuePoint : data) {
            String title = StringUtils.capitalizeEveryWord(
                    cuePoint.getString(RadioPlayback.CUE_TITLE));
            String artist = StringUtils.capitalizeEveryWord(
                    cuePoint.getString(RadioPlayback.TRACK_ARTIST_NAME));
            if (title != null && artist != null) {
                Track track = new Track(title, artist);
                mCuePointDescriptionList.add(track);

                // Add the track to the list of pending image url requests
                mPendingCoverArtRequests.addLast(track);
            }
        }
        mAdapter.setData(mCuePointDescriptionList);
        mAdapter.notifyDataSetChanged();

        // Hide the error state
        updateErrorState();

        // Re-enable the SwipeRefreshLayout
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setEnabled(true);

        // Start fetching the cover art image urls
        maybeFetchNextCoverArtImageUrl();
    }

    @Override
    public void onCuePointHistoryFailed(CuePointHistory cuePointHistory, int errorCode) {
        // Show the error state
        updateErrorState();

        // Re-enable the SwipeRefreshLayout
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setEnabled(true);
    }

    /**
     * Checks {@link #mPendingCoverArtRequests} to see if there are any pending Tracks that need a
     * to fetch a cover art url
     */
    public void maybeFetchNextCoverArtImageUrl() {
        if (!mPendingCoverArtRequests.isEmpty()) {
            // Remove the last track
            Track track = mPendingCoverArtRequests.removeFirst();

            // Get the cover art url for that track
            // Note: getCoverArtUrl will call maybeFetchNextCoverArtImageUrl after it receives a
            // response from the MusicBrainzApi
            getCoverArtUrl(track);
        }
    }

    private void getCoverArtUrl(final Track track) {
        // Get the cover art url for the track
        String coverArtUrl = mUrlCache.getCoverArtUrl(track);
        if (coverArtUrl == null) {
            // Cache miss, so fetch the url for the track
            mUrlCache.fetchCoverArtUrl(track, new CoverArtUrlCache.CacheResultListener() {
                @Override
                public void onResult(@Nullable String coverArtUrl) {
                    if (coverArtUrl != null) {
                        addCoverArtUrlToTrack(track, coverArtUrl);
                    }
                    /*
                    else {
                        Log.d("====", track.getTitle() + " by " + track.getArtist() + " has null coverArtUrl");
                    }
                    */
                }
            });
        } else {
            // Cache hit, so instantly add the url to the track
            addCoverArtUrlToTrack(track, coverArtUrl);
        }

        // Get the next image, if needed
        maybeFetchNextCoverArtImageUrl();
    }

    private void addCoverArtUrlToTrack(final Track track, String coverArtUrl) {
        track.setCoverArtUrl(coverArtUrl);

        // Replace the track in the list
        int idx = mCuePointDescriptionList.indexOf(track);
        if (idx != -1) {
            mCuePointDescriptionList.remove(idx);
            mCuePointDescriptionList.add(idx, track);

            // Only update this specific item in the adapter
            mAdapter.notifyItemChanged(idx);
        }
    }

    @Override
    public void onRefresh() {
        refreshRecentlyPlayedList();
    }

    private void refreshRecentlyPlayedList() {
        mCuePointHistory.request();
        mSwipeRefreshLayout.setEnabled(false);
    }

    @Override
    public boolean onSongFavoriteUpdate(Favorite favorite) {
        return maybeUpdateCurrentSongFavorite(favorite);
    }

    private void updateErrorState() {
        if (mAdapter.getItemCount() <= 0) {
            mErrorLayout.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mErrorLayout.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}