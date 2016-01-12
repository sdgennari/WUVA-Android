package com.poofstudios.android.wuvaradio.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.poofstudios.android.wuvaradio.RadioPlayerService;

/**
 * Base activity for activities that involve media playback. This class handles binding to the
 * RadioPlayerService and creates a new media session for the app. The media controller for the
 * session is set on the activity, so subclasses can access it with getSupportMediaController()
 */
public abstract class MediaBaseActivity extends AppCompatActivity {

    protected RadioPlayerService mService;
    protected boolean mServiceBound = false;

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

    /**
     * Connects to the current MediaSession to receive callbacks
     * @param token token from the current MediaSession
     */
    private void connectToSession(MediaSessionCompat.Token token) {
        Log.d("====", "connectToSession");
        try {
            MediaControllerCompat mediaController = new MediaControllerCompat(MediaBaseActivity.this, token);
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

    /**
     * Updates UI based on the playback state
     * @param playbackState current playback state
     */
    protected abstract void updatePlaybackState(PlaybackStateCompat playbackState);

    /**
     * Updates the UI with the new media description
     * @param description new MediaDescription from the session metadata
     */
    protected abstract void updateMediaDescription(MediaDescriptionCompat description);
}
