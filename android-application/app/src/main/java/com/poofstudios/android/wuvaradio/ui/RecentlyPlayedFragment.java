package com.poofstudios.android.wuvaradio.ui;

import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.RadioPlayback;
import com.poofstudios.android.wuvaradio.utils.StringUtils;
import com.tritondigital.player.CuePoint;
import com.tritondigital.player.CuePointHistory;

import java.util.ArrayList;
import java.util.List;

public class RecentlyPlayedFragment extends MediaBaseFragment implements
        CuePointHistory.CuePointHistoryListener {

    private RecyclerView mRecyclerView;
    private HistoryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private CuePointHistory mCuePointHistory;

    private List<MediaDescriptionCompat> mCuePointDescriptionList;

    public RecentlyPlayedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCuePointDescriptionList = new ArrayList<>();

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

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

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
        mCuePointDescriptionList.add(0, description);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCuePointHistoryReceived(CuePointHistory cuePointHistory, List<Bundle> data) {
        mCuePointDescriptionList = new ArrayList<>();
        for (Bundle cuePoint : data) {
            String title = StringUtils.capitalizeEveryWord(
                    cuePoint.getString(RadioPlayback.CUE_TITLE));
            String artist = StringUtils.capitalizeEveryWord(
                    cuePoint.getString(RadioPlayback.TRACK_ARTIST_NAME));
            if (title != null && artist != null) {
                MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title);
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist);

                // TODO Handle cover art url

                mCuePointDescriptionList.add(metadataBuilder.build().getDescription());
            }
        }
        mAdapter.setData(mCuePointDescriptionList);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCuePointHistoryFailed(CuePointHistory cuePointHistory, int errorCode) {
        // TODO Show error state
    }
}