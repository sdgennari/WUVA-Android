package com.poofstudios.android.wuvaradio;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.poofstudios.android.wuvaradio.api.MusicBrainzApi;
import com.poofstudios.android.wuvaradio.api.MusicBrainzService;
import com.poofstudios.android.wuvaradio.api.model.RecordingResponse;
import com.poofstudios.android.wuvaradio.utils.UrlUtils;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class RadioPlayerService extends Service implements
        AudioManager.OnAudioFocusChangeListener,
        RadioPlayback.Callback {

    public static final String CMD_PLAY = "CMD_PLAY";

    private final IBinder mBinder = new LocalBinder();
    private AudioManager mAudioManager = null;
    private MusicBrainzService musicBrainzService = null;
    private MediaNotificationManager mMediaNotificationManager;

    private RadioPlayback mPlayback;

    // Media Sessions
    private MediaSessionCompat mMediaSession = null;
    private static final String MEDIA_SESSION_TAG = "WUVAMediaSession";
    private static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PLAY |
            PlaybackStateCompat.ACTION_STOP;

    private static final boolean ALLOW_REBIND = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        musicBrainzService = MusicBrainzApi.getService();

        mPlayback = new RadioPlayback(this);
        mPlayback.setCallback(this);

        // Start new MediaSession
        // Create an instance of the MediaButtonReceiver class
        ComponentName mediaButtonReceiver = new ComponentName(this, RemoteControlReceiver.class);
        MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
        mMediaSession = new MediaSessionCompat(this,
                MEDIA_SESSION_TAG,
                mediaButtonReceiver,        // Include mediaButtonReceiver to support pre-L devices
                null);
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(mediaSessionCallback);

        mMediaNotificationManager = new MediaNotificationManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (CMD_PLAY.equals(action)) {
            // Requesting audio focus will handle creating the player and playing the stream
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlayback.play();
            } else {
                Log.d("WUVA", "AudioFocus Gain Not Granted");
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return ALLOW_REBIND;
    }

    @Override
    public void onDestroy() {
        if (mAudioManager != null) {
            int result = mAudioManager.abandonAudioFocus(this);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mPlayback.stop();
                mPlayback.release();
            } else {
                Log.d("WUVA", "AudioFocus Abandon Not Granted");
            }
        }
        stopForeground(true);
        mMediaSession.release();
        super.onDestroy();
    }

    @Nullable
    public MediaSessionCompat.Token getSessionToken() {
        return mMediaSession.getSessionToken();
    }

    private void getCoverArtUrl(String title, String artist) {
        String query = UrlUtils.formatMusicBrainzQuery(title, artist);
        Call<RecordingResponse> recordingResponseCall = musicBrainzService.getMBID(query);
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
                            //currentCoverArtUrl = UrlUtils.formatCoverArtUrl(release.id);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("WUVA", t.getLocalizedMessage());
            }
        });
    }

    private void updateMediaSessionMetadata(MediaMetadataCompat metadata) {
        Log.d("====", "updateMediaSessionMetadata");
        // Update the metadata for the session
        mMediaSession.setMetadata(metadata);

        // TODO Fetch cover art url
//        // Query for the new cover art
//        getCoverArtUrl(title, artist);  // Callback will set currentCoverArtUrl
    }

    private void updatePlaybackState() {
        Log.d("====", "updatePlaybackState " + mPlayback.getState());
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PLAYBACK_ACTIONS);

        int state = mPlayback.getState();
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        stateBuilder.setState(state, position, 1.0f);

        mMediaSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            mMediaNotificationManager.startNotification();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch(focusChange) {
            // Gained focus to play music
            case AudioManager.AUDIOFOCUS_GAIN:
                mPlayback.play();
                break;

            // Lost focus for a long period of time, so release memory
            case AudioManager.AUDIOFOCUS_LOSS:
                mPlayback.stop();
                mPlayback.release();
                break;

            // Lost focus for a short time, will regain soon
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mPlayback.stop();
                break;

            // Lost focus for a short time, but can still play quietly
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mPlayback.duck();
                break;
        }
        updatePlaybackState();
    }


    public class LocalBinder extends Binder {
        RadioPlayerService getService() {
            // Return this instance of the service to expose public methods
            return RadioPlayerService.this;
        }
    }

    // Implementation of RadioPlayback Callback interface
    /**
     * Called when playback updates playback state. Overrides method from RadioPlayback
     * Callback interface
     */
    @Override
    public void onPlaybackStateChanged() {
        updatePlaybackState();
    }

    /**
     * Called when playback receives new metadata. Overrides method from RadioPlayback
     * Callback interface
     * @param metadata metadata from the stream
     */
    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        Log.d("====", "onMetadataChanged");
        updateMediaSessionMetadata(metadata);
    }

    /**
     * Receives transport controls, media buttons, etc. that affect media playback
     */
    private final class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            mPlayback.play();
        }

        @Override
        public void onStop() {
            Log.d("====", "MediaSessionCallback onStop");
            mPlayback.stop();
            mPlayback.release();

            // Playback ended, so stop the service
            RadioPlayerService.this.stopSelf();
        }

    }
}
