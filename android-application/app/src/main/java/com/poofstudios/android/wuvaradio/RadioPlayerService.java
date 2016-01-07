package com.poofstudios.android.wuvaradio;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.poofstudios.android.wuvaradio.api.MusicBrainzApi;
import com.poofstudios.android.wuvaradio.api.MusicBrainzService;
import com.poofstudios.android.wuvaradio.api.model.RecordingResponse;
import com.poofstudios.android.wuvaradio.utils.StringUtils;
import com.poofstudios.android.wuvaradio.utils.UrlUtils;
import com.tritondigital.player.MediaPlayer;
import com.tritondigital.player.TritonPlayer;

import java.util.HashMap;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class RadioPlayerService extends Service implements
        MediaPlayer.OnCuePointReceivedListener,
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnStateChangedListener {

    // LocalBroadcastManager fields
    public static final String INTENT_UPDATE_COVER_ART = "INTENT_UPDATE_COVER_ART";
    public static final String INTENT_UPDATE_TITLE_ARTIST = "INTENT_UPDATE_TITLE_ARTIST";
    public static final String EXTRA_COVER_ART_URL = "EXTRA_COVER_ART_URL";
    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    public static final String EXTRA_ARTIST = "EXTRA_ARTIST";

    private static final String CUE_TITLE = "cue_title";
    private static final String TRACK_ARTIST_NAME = "track_artist_name";

    public static final String CMD_PLAY = "CMD_PLAY";

    // TODO Get actual Broadcaster and Name from WUVA
    private static final String STATION_BROADCASTER = "WUVA";
    private static final String STATION_NAME = "WUVA";
    private static final String STATION_MOUNT = "WUVA";

    private final IBinder mBinder = new LocalBinder();
    private TritonPlayer mPlayer = null;
    private AudioManager mAudioManager = null;
    private MusicBrainzService musicBrainzService = null;
    private LocalBroadcastManager localBroadcastManager = null;
    private MediaNotificationManager mMediaNotificationManager;

    // Media Sessions
    private MediaSessionCompat mMediaSession = null;
    private static final String MEDIA_SESSION_TAG = "WUVAMediaSession";
    private static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PLAY |
            PlaybackStateCompat.ACTION_STOP;

    private boolean isForeground = false;
    private static final boolean ALLOW_REBIND = true;

    private String currentArtist;
    private String currentTitle;
    private String currentCoverArtUrl;

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        musicBrainzService = MusicBrainzApi.getService();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Start new MediaSession
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

        this.currentArtist = null;
        this.currentTitle = null;
        this.currentCoverArtUrl = null;
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
                startPlayer();
                updatePlaybackState();
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
                stopPlayer();
                releasePlayer();
            } else {
                Log.d("WUVA", "AudioFocus Abandon Not Granted");
            }
        }
        stopForeground(true);
        isForeground = false;
        mMediaSession.release();
        super.onDestroy();
    }

    private void stopPlayer() {
        if (mPlayer != null && mPlayer.getState() == TritonPlayer.STATE_PLAYING) {
            mPlayer.stop();
        }
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void duckPlayer() {
        if (mPlayer != null && mPlayer.getState() == TritonPlayer.STATE_PLAYING) {
            mPlayer.setVolume(TritonPlayer.VOLUME_DUCK);
        }
    }

    private void startPlayer() {
        if (mPlayer == null || mPlayer.getState() == TritonPlayer.STATE_RELEASED) {
            createPlayer();
        } else if (mPlayer.getState() != TritonPlayer.STATE_PLAYING) {
            mPlayer.play();
        }
        mPlayer.setVolume(TritonPlayer.VOLUME_NORMAL);

        // Set MediaSession to active
        // Tells system to route controls to the app
        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }
    }

    private void createPlayer() {
        // Configure player settings
        Bundle settings = new Bundle();
        settings.putString(TritonPlayer.SETTINGS_STATION_BROADCASTER, STATION_BROADCASTER);
        settings.putString(TritonPlayer.SETTINGS_STATION_NAME, STATION_NAME);
        settings.putString(TritonPlayer.SETTINGS_STATION_MOUNT, STATION_MOUNT);

        // Create and start a new player
        if (mPlayer != null) {
            mPlayer.release();
        }
        mPlayer = new TritonPlayer(this, settings);
        mPlayer.setOnCuePointReceivedListener(this);
        mPlayer.setOnStateChangedListener(this);
        mPlayer.play();
    }

    @Nullable
    public MediaSessionCompat.Token getSessionToken() {
        return mMediaSession.getSessionToken();
    }

    public String getCurrentArtist() {
        return this.currentArtist;
    }

    public String getCurrentTitle() {
        return this.currentTitle;
    }

    public String getCurrentCoverArtUrl() {
        return this.currentCoverArtUrl;
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
                            currentCoverArtUrl = UrlUtils.formatCoverArtUrl(release.id);

                            // Send a broadcast with url for cover art image
                            HashMap<String, String> messageData = new HashMap<>();
                            messageData.put(EXTRA_COVER_ART_URL, currentCoverArtUrl);
                            sendMessage(INTENT_UPDATE_COVER_ART, messageData);
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

    private void updateMediaSessionMetadata() {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentTitle);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentArtist);
        // TODO Handle album art
        mMediaSession.setMetadata(metadataBuilder.build());
    }

    private void updatePlaybackState() {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PLAYBACK_ACTIONS);

        int state = PlaybackStateCompat.STATE_ERROR;
        switch (mPlayer.getState()) {
            case TritonPlayer.STATE_PLAYING:
            case TritonPlayer.STATE_CONNECTING:
                state = PlaybackStateCompat.STATE_PLAYING;
                break;
            case TritonPlayer.STATE_STOPPED:
                state = PlaybackStateCompat.STATE_STOPPED;
                break;
        }
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        stateBuilder.setState(state, position, 1.0f);

        mMediaSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            mMediaNotificationManager.startNotification();
        }
    }

    /**
     * Sends a broadcast from the service to any local activities
     *
     * @param eventName Name of the event being broadcast
     * @param data Extras to be put in the intent
     */
    private void sendMessage(String eventName, HashMap<String, String> data) {
        Intent broadcastIntent = new Intent(eventName);
        for (String key : data.keySet()) {
            String value = data.get(key);
            broadcastIntent.putExtra(key, value);
        }
        localBroadcastManager.sendBroadcast(broadcastIntent);
    }

    @Override
    public void onCuePointReceived(MediaPlayer mediaPlayer, Bundle cuePoint) {
        if (mPlayer == mediaPlayer) {
            if (cuePoint != null) {
                String title = StringUtils.capitalizeEveryWord(cuePoint.getString(CUE_TITLE));
                String artist = StringUtils.capitalizeEveryWord(cuePoint.getString(TRACK_ARTIST_NAME));
                if (title != null && artist != null) {
                    // Update current values
                    this.currentArtist = artist;
                    this.currentTitle = title;
                    this.currentCoverArtUrl = "";     // Reset value until callback received

                    // Send a broadcast with the new title and artist
                    HashMap<String, String> messageData = new HashMap<>();
                    messageData.put(EXTRA_TITLE, title);
                    messageData.put(EXTRA_ARTIST, artist);
                    sendMessage(INTENT_UPDATE_TITLE_ARTIST, messageData);
                    updateMediaSessionMetadata();

                    // Query for the new cover art
                    getCoverArtUrl(title, artist);  // Callback will set currentCoverArtUrl
                }
            }
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch(focusChange) {
            // Gained focus to play music
            case AudioManager.AUDIOFOCUS_GAIN:
                startPlayer();
                break;

            // Lost focus for a long period of time, so release memory
            case AudioManager.AUDIOFOCUS_LOSS:
                stopPlayer();
                releasePlayer();
                break;

            // Lost focus for a short time, will regain soon
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                stopPlayer();
                break;

            // Lost focus for a short time, but can still play quietly
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                duckPlayer();
                break;
        }
        updatePlaybackState();
    }

    @Override
    public void onStateChanged(MediaPlayer mediaPlayer, int state) {
        updatePlaybackState();
    }

    public class LocalBinder extends Binder {
        RadioPlayerService getService() {
            // Return this instance of the service to expose public methods
            return RadioPlayerService.this;
        }
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        // Requests to begin playback
        @Override
        public void onPlay() {
            Log.d("====", "MediaSessionCallback onPlay");
            startPlayer();
        }

        // Requests to stop playback
        @Override
        public void onStop() {
            Log.d("====", "MediaSessionCallback onStop");
            stopPlayer();
        }

    }
}
