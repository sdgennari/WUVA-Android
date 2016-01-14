package com.poofstudios.android.wuvaradio.ui;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.poofstudios.android.wuvaradio.R;

import java.util.ArrayList;
import java.util.List;

public class TabActivity extends MediaBaseActivity {

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private PlaybackControlsFragment mPlaybackControlsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPlaybackControlsFragment = (PlaybackControlsFragment) getSupportFragmentManager()
                 .findFragmentById(R.id.fragment_playback_controls);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new RadioFragment(), getString(R.string.title_radio));
        adapter.addFragment(new FavoriteFragment(), getString(R.string.title_favorite));
        adapter.addFragment(new RecentlyPlayedFragment(), getString(R.string.title_recently_played));
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(adapter);

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    // Override methods from MediaBaseActivity
    @Override
    protected void onSessionConnected() {
        super.onSessionConnected();

        if (mPlaybackControlsFragment != null) {
            mPlaybackControlsFragment.onControllerConnected();
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

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

}
