package com.poofstudios.android.wuvaradio.ui;

import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.RadioPlayback;
import com.poofstudios.android.wuvaradio.api.MusicBrainzApi;
import com.poofstudios.android.wuvaradio.api.MusicBrainzService;
import com.poofstudios.android.wuvaradio.api.model.RecordingResponse;
import com.poofstudios.android.wuvaradio.model.Track;
import com.poofstudios.android.wuvaradio.utils.StringUtils;
import com.poofstudios.android.wuvaradio.utils.UrlUtils;
import com.tritondigital.player.CuePoint;
import com.tritondigital.player.CuePointHistory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class RecentlyPlayedFragment extends MediaBaseFragment implements
        CuePointHistory.CuePointHistoryListener,
        SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private HistoryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private CuePointHistory mCuePointHistory;

    private List<Track> mCuePointDescriptionList;
    private ArrayDeque<Track> mPendingCoverArtRequests;

    // For getting album cover art
    private MusicBrainzService mMusicBrainzService;

    public RecentlyPlayedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCuePointDescriptionList = new ArrayList<>();
        mPendingCoverArtRequests = new ArrayDeque<>();

        mMusicBrainzService = MusicBrainzApi.getService();

        mCuePointHistory = new CuePointHistory();
        mCuePointHistory.setListener(this);
        mCuePointHistory.setCueTypeFilter(CuePoint.CUE_TYPE_VALUE_TRACK);
        mCuePointHistory.setMaxItems(15);
        mCuePointHistory.setMount(RadioPlayback.STATION_MOUNT);

        mCuePointHistory.request();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recently_played, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setRefreshing(true);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LineDividerDecoration(getActivity(), getResources()));

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HistoryAdapter();
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

        // Re-enable the SwipeRefreshLayout
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setEnabled(true);

        // Start fetching the cover art image urls
        maybeFetchNextCoverArtImageUrl();
    }

    @Override
    public void onCuePointHistoryFailed(CuePointHistory cuePointHistory, int errorCode) {
        // TODO Show error state

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

    // Mostly copied from RadioPlayerService

    /**
     * Fetches the cover art url for a track using the MusicBrainz API. When the request returns,
     * this method calls {@link #maybeFetchNextCoverArtImageUrl()} to fetch the next url in the list
     * of pending requests
     * @param track track to get cover art url for
     */
    private void getCoverArtUrl(final Track track) {
        // Formats query
        String query = UrlUtils.formatMusicBrainzQuery(track.getTitle(), track.getArtist());

        // Create callback object
        Call<RecordingResponse> recordingResponseCall = mMusicBrainzService.getMBID(query);

        // Enqueue query, handle response
        recordingResponseCall.enqueue(new Callback<RecordingResponse>() {
            @Override
            public void onResponse(Response<RecordingResponse> response, Retrofit retrofit) {
                // Response not null
                if (response != null && response.body() != null) {
                    RecordingResponse recordingResponse = response.body();
                    // At least one recording returned
                    if (recordingResponse.count != 0) {
                        // Get the first recording
                        RecordingResponse.Recording recording = recordingResponse.recordings.get(0);

                        // At least one release
                        if (recording.releases.size() != 0) {
                            RecordingResponse.Release release = recording.releases.get(0);
                            String coverArtUrl = UrlUtils.formatCoverArtUrl(release.id);

                            track.setCoverArtUrl(coverArtUrl);
                            int idx = mCuePointDescriptionList.indexOf(track);
                            // Replace the track in the list
                            if (idx != -1) {
                                mCuePointDescriptionList.remove(idx);
                                mCuePointDescriptionList.add(idx, track);

                                // Only update this specific item in the adapter
                                mAdapter.notifyItemChanged(idx);
                            }
                        }
                    }
                }

                // Get the next image, if needed
                maybeFetchNextCoverArtImageUrl();
            }

            @Override
            public void onFailure(Throwable t) {
                // Get the next image, if needed
                maybeFetchNextCoverArtImageUrl();

                Log.e("WUVA", "Error: " + t.getLocalizedMessage());
            }
        });
    }

    @Override
    public void onRefresh() {
        mCuePointHistory.request();
        mSwipeRefreshLayout.setEnabled(false);
    }
}