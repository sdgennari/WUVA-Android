package com.poofstudios.android.wuvaradio.ui;

import android.os.Bundle;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.Toolbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;

import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.poofstudios.android.wuvaradio.R;

public class MainActivity extends MediaBaseActivity {

    private PlaybackControlsFragment mPlaybackControlsFragment;

    // Nav drawer
    private String[] mNavItems;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //mToolbar.setTitle(mTitle);
        //mToolbar.getBackground().setAlpha(0);
        mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.darkTextColorPrimary));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        // Nav drawer
        mNavItems = new String[] {"Radio", "Favorites", "Recently Played"};
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, mNavItems));

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                mToolbar,
                R.string.drawer_open,
                R.string.drawer_close);

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerToggle.syncState();

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Set the radio fragment as the primary fragment
        Fragment radioFragment = new RadioFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, radioFragment)
                .commit();

        // Get the playback control fragment from the ui
        mPlaybackControlsFragment = (PlaybackControlsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    // Override methods from MediaBaseActivity
    @Override
    protected void onSessionConnected() {
        super.onSessionConnected();

        if (mPlaybackControlsFragment != null) {
            mPlaybackControlsFragment.onControllerConnected();
        }

        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof MediaBaseFragment) {
            ((MediaBaseFragment) currentFragment).onControllerConnected();
        }


    }

    @Override
    protected void updatePlaybackState(PlaybackStateCompat playbackState) {
        if (shouldShowPlaybackControls()) {
            showPlaybackControls();
        } else {
            hidePlaybackControls();
        }
    }

    @Override
    protected void updateMediaDescription(MediaDescriptionCompat description) {
        if (shouldShowPlaybackControls()) {
            showPlaybackControls();
        } else {
            hidePlaybackControls();
        }
    }

    private boolean shouldShowPlaybackControls() {
        MediaControllerCompat controller = getSupportMediaController();

        // Check for valid data
        if (controller == null || controller.getPlaybackState() == null ||
                controller.getMetadata() == null) {
            return false;
        }

        // If the player has been released, hide controls
        // Else show controls
        int state = controller.getPlaybackState().getState();
        if (state == PlaybackStateCompat.STATE_ERROR ||
                state == PlaybackStateCompat.STATE_NONE) {
            return false;
        }
        return true;
    }

    private void hidePlaybackControls() {
        if (mPlaybackControlsFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .hide(mPlaybackControlsFragment)
                    .commit();
        }
    }

    private void showPlaybackControls() {
        if (mPlaybackControlsFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .show(mPlaybackControlsFragment)
                    .commit();
        }
    }
}
