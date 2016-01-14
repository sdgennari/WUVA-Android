package com.poofstudios.android.wuvaradio;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ToggleButton;
import android.widget.TextView;

import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.widget.Button;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.poofstudios.android.wuvaradio.utils.BlurTransform;
import com.squareup.picasso.Target;

public class MainActivity extends AppCompatActivity {

    private RadioPlayerService mService;
    private boolean mServiceBound = false;

    /*Button mStopButton;
    Button mStartButton;*/
    ToggleButton mStartStopButton;

    // Nav drawer
    private String[] mNavItems;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private Toolbar mToolbar;

    LinearLayout mActivityContent;
    ImageView mCoverArtView;
    TextView mTitleView;
    TextView mArtistView;

    String mTitle;
    String mArtist;
    String mCoverArtUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbar.setTitle(mTitle);
        //mToolbar.getBackground().setAlpha(0);
        mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        mActivityContent = (LinearLayout) findViewById(R.id.activity_content);
        mCoverArtView = (ImageView) findViewById(R.id.cover_art);
        mTitleView = (TextView) findViewById(R.id.title);
        mArtistView = (TextView) findViewById(R.id.artist);

        // Nav drawer
        mNavItems = new String[] {"Radio", "Favorites", "Recently Played"};
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
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

        // New start/stop
        mStartStopButton = (ToggleButton) findViewById(R.id.start_stop_button);
        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStartStopButton.isChecked()) {
                    startService();
                } else {
                    MediaControllerCompat.TransportControls controls =
                            getSupportMediaController().getTransportControls();
                    controls.stop();
                }
            }
        });

        // Load placeholder cover art
        Picasso.with(this).load(R.drawable.cover_art_placeholder).fit().centerInside().into(mCoverArtView);
        mActivityContent.setBackgroundColor(Color.parseColor("#000000"));
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

    private void startService() {
        Intent intent = new Intent(this, RadioPlayerService.class);
        intent.setAction(RadioPlayerService.CMD_PLAY);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to the service
        Intent bindIntent = new Intent(this, RadioPlayerService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
        if (getSupportMediaController() != null) {
            getSupportMediaController().unregisterCallback(mControllerCallback);
        }
    }

    private void doUnbindService() {
        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Connects to the current MediaSession to receive callbacks
     * @param token token from the current MediaSession
     */
    private void connectToSession(MediaSessionCompat.Token token) {
        Log.d("====", "connectToSession");
        try {
            MediaControllerCompat mediaController = new MediaControllerCompat(MainActivity.this, token);
            setSupportMediaController(mediaController);
            mediaController.registerCallback(mControllerCallback);

            // Update the playback state
            PlaybackStateCompat playbackState = mediaController.getPlaybackState();
            updatePlaybackState(playbackState);

            // Update the metadata description
            MediaMetadataCompat metadata = mediaController.getMetadata();
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
            }
        } catch (RemoteException e) {
            Log.e("WUVA", e.getLocalizedMessage());
        }

    }

    /**
     * Updates album cover and text with currently playing song
     */
    private void updateUI() {
        mArtistView.setText(mArtist);
        mTitleView.setText(mTitle);

        if(!mCoverArtUrl.isEmpty()) {
            Picasso.with(this).load(mCoverArtUrl).placeholder(R.drawable.cover_art_placeholder).fit().centerInside().into(mCoverArtView);

            Picasso.with(this).load(mCoverArtUrl).transform(new BlurTransform(this)).resize(100,100).centerCrop().into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    mActivityContent.setBackground(new BitmapDrawable(getResources(), bitmap));
                }

                @Override
                public void onBitmapFailed(final Drawable errorDrawable) {
                    Log.d("TAG", "FAILED");
                }

                @Override
                public void onPrepareLoad(final Drawable placeHolderDrawable) {
                    Log.d("TAG", "Prepare Load");
                }
            });


        } else {
            Picasso.with(this).load(R.drawable.cover_art_placeholder).fit().centerInside().into(mCoverArtView);
            mActivityContent.setBackgroundColor(Color.parseColor("#000000"));
        }

        Log.d("url", mCoverArtUrl);
    }

    /**
     * Updates UI based on the playback state
     * @param playbackState current playback state
     */
    private void updatePlaybackState(PlaybackStateCompat playbackState) {
        if (playbackState == null) {
            return;
        }

        // Update the UI based on the current playback state
        switch(playbackState.getState()) {
            case PlaybackStateCompat.STATE_CONNECTING:
                mArtistView.setText("Connecting...");
                mTitleView.setText("");
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                updateUI();
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                mArtistView.setText("Playback stopped.");
                mTitleView.setText("");
                break;
            default:
                Log.d("WUVA", "Unhandled state " + playbackState.getState());
        }


        // Can handle other button visibility with playbackState.getActions() or
        // playbackState.getCustomActions()
    }

    /**
     * Updates variables with the new media description
     * @param description new MediaDescription from the session metadata
     */
    private void updateMediaDescription(MediaDescriptionCompat description) {
        Log.d("====", "updateMediaDescription");
        if (description == null) {
            return;
        }

        // Update the variables
        mTitle = description.getTitle().toString();
        mArtist = description.getSubtitle().toString();

        if (description.getIconUri() != null) {
            mCoverArtUrl = description.getIconUri().toString();
        } else {
            // Request is still pending, so no MediaUri set
            mCoverArtUrl = "";
        }

        updateUI();
    }

    /**
     * Defines callbacks for service binding
     * Passed as a param to bindService()
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("====", "onServiceConnected");
            mService = ((RadioPlayerService.LocalBinder) service).getService();
            mServiceBound = true;

            connectToSession(mService.getSessionToken());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };

    /**
     * Callback for MediaController to update the notification when the session changes
     */
    private final MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
            }
        }
    };
}
