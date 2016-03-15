package com.poofstudios.android.wuvaradio.ui;

import android.os.Bundle;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;

import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.poofstudios.android.wuvaradio.R;

public class MainActivity extends MediaBaseActivity {

    private PlaybackControlsFragment mPlaybackControlsFragment;
    private CardView mPlaybackControlsContainer;

    // Nav drawer item titles
    private static final String NAV_RADIO = "Radio";
    private static final String NAV_FAVORITES = "Favorites";
    private static final String NAV_RECENTLY_PLAYED = "Recently Played";

    // Nav drawer
    private String[] mNavItems;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mSelectedItem;

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
        mNavItems = new String[] {NAV_RADIO, NAV_FAVORITES, NAV_RECENTLY_PLAYED};
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectNavItem(position);
            }
        });

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
        selectNavItem(0);

        // Get the playback control fragment from the ui
        mPlaybackControlsFragment = (PlaybackControlsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);
        mPlaybackControlsContainer = (CardView) findViewById(R.id.controls_container);
    }

    private void selectNavItem(int position) {
        String navItem = mNavItems[position];
        mSelectedItem = navItem;

        // Create a new fragment based on selection
        Fragment fragment;
        switch (navItem) {
            case NAV_FAVORITES:
                fragment = new FavoriteFragment();
                break;
            case NAV_RECENTLY_PLAYED:
                fragment = new RecentlyPlayedFragment();
                break;
            case NAV_RADIO:
            default:
                fragment = new RadioFragment();
        }

        // Replace the current fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        // Show control fragment if necessary
        if (shouldShowPlaybackControls()) {
            showPlaybackControls();
        } else {
            hidePlaybackControls();
        }

        // Highlight the item in nav drawer
        mDrawerList.setItemChecked(position, true);

        // Update the toolbar
        setTitle(navItem);

        // Close the nav drawer
        mDrawerLayout.closeDrawer(mDrawerList);

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

        // Notify the controller fragment that a session has started
        if (mPlaybackControlsFragment != null) {
            mPlaybackControlsFragment.onControllerConnected();
        }

        // Notify the current fragment that the session has been connected
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

        // Do not show controls on the radio screen
        if (mSelectedItem.equals(NAV_RADIO)) {
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
                    .commitAllowingStateLoss();
        }
        if (mPlaybackControlsContainer != null) {
            mPlaybackControlsContainer.setVisibility(View.GONE);
        }
    }

    private void showPlaybackControls() {
        if (mPlaybackControlsFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .show(mPlaybackControlsFragment)
                    .commitAllowingStateLoss();
        }
        if (mPlaybackControlsContainer != null) {
            mPlaybackControlsContainer.setVisibility(View.VISIBLE);
        }
    }
}
