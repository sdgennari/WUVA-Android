package com.poofstudios.android.wuvaradio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private RadioPlayerService mService;
    private boolean mServiceBound = false;

    Button mStopButton;
    Button mStartButton;
    TextView mTitleView;
    TextView mArtistView;
    TextView mCoverArtUrlView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mTitleView = (TextView) findViewById(R.id.title);
        mArtistView = (TextView) findViewById(R.id.artist);
        mCoverArtUrlView = (TextView) findViewById(R.id.cover_art_url);

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService();
            }
        });

        mStopButton = (Button) findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                        getSupportMediaController().getTransportControls();
                controls.stop();
            }
        });
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
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private void updatePlaybackState(PlaybackStateCompat playbackState) {
        if (playbackState == null) {
            return;
        }

        // Update the UI based on the current playback state
        switch(playbackState.getState()) {
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_PLAYING:
                mStartButton.setVisibility(View.GONE);
                mStopButton.setVisibility(View.VISIBLE);
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                mStartButton.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.GONE);
                break;
            default:
                Log.d("WUVA", "Unhandled state " + playbackState.getState());
        }
        // Can handle other button visibility with playbackState.getActions() or
        // playbackState.getCustomActions()
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        Log.d("====", "updateMediaDescription");
        if (description == null) {
            return;
        }
        // Update the views
        mTitleView.setText(description.getTitle());
        mArtistView.setText(description.getSubtitle());
        mCoverArtUrlView.setText("Pending...");
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
