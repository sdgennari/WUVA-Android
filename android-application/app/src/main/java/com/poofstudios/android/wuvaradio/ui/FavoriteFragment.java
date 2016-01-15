package com.poofstudios.android.wuvaradio.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.model.Favorite;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class FavoriteFragment extends MediaBaseFragment {

    private RecyclerView mRecyclerView;
    private FavoriteAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorite, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LineDividerDecoration(getActivity(), getResources()));

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new FavoriteAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    public FavoriteFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
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