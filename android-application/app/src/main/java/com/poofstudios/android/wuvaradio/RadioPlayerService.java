package com.poofstudios.android.wuvaradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
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

public class RadioPlayerService extends Service implements MediaPlayer.OnCuePointReceivedListener,
    AudioManager.OnAudioFocusChangeListener {

    // LocalBroadcastManager fields, passed between service and activity
    public static final String INTENT_UPDATE_COVER_ART = "INTENT_UPDATE_COVER_ART";
    public static final String INTENT_UPDATE_TITLE_ARTIST = "INTENT_UPDATE_TITLE_ARTIST";
    public static final String EXTRA_COVER_ART_URL = "EXTRA_COVER_ART_URL";
    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    public static final String EXTRA_ARTIST = "EXTRA_ARTIST";

    // Notification fields, passed between notification and service
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final int NOTIFICATION_ID = 777;

    // Meta data from Triton
    private static final String CUE_TITLE = "cue_title";
    private static final String TRACK_ARTIST_NAME = "track_artist_name";

    // Fields given to Triton to establish radio connection
    // TODO Get actual Broadcaster and Name from WUVA
    private static final String STATION_BROADCASTER = "WUVA";
    private static final String STATION_NAME = "WUVA";
    private static final String STATION_MOUNT = "WUVA";

    // Binds activity to service
    private final IBinder mBinder = new LocalBinder();

    // Establishes radio connection
    private TritonPlayer mPlayer = null;

    // Current song data
    private String currentArtist;
    private String currentTitle;
    private String currentCoverArtUrl;

    // For getting album cover art
    private MusicBrainzService musicBrainzService = null;
    private LocalBroadcastManager localBroadcastManager = null;

    // Manages notifications
    private NotificationManager mNotificationManager = null;

    // Handles audio focus (phone, GPS, etc.)
    private AudioManager mAudioManager = null;

    private boolean isForeground = false;
    private static final boolean ALLOW_REBIND = true;

    /**
     * Called when service starts
     * @param intent Intent passed to service via startService() method (may be null)
     * @param flags Additional data about start request
     * @param startId Unique ID representing specific start request
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        // Play radio if play action given
        if (ACTION_PLAY.equals(action)) {
            // Requesting audio focus will handle creating the player and playing the stream
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startPlayer();
            } else {
                Log.d("WUVA", "AudioFocus Gain Not Granted");
            }
        }
        return START_NOT_STICKY;
    }

    /*
     * Callback methods for binding lifecycle
     */
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

    /**
     * Initializes managers, MusicBrainz service
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        musicBrainzService = MusicBrainzApi.getService();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        this.currentArtist = null;
        this.currentTitle = null;
        this.currentCoverArtUrl = null;
    }

    /**
     * Stops player, releases media player instance
     */
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
        super.onDestroy();
    }

    /**
     * Builds notification
     */
    private Notification createNotification() {
        String ticker = String.format("%s by %s", currentTitle, currentArtist);

        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setTicker(ticker)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    /*
     * Player actions
     */
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

    // Makes player quiet (for phone, GPS, etc.)
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
    }

    /**
     * Builds music player
     */
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
        mPlayer.play();
    }

    /*
     * Getter methods
     */
    public String getCurrentArtist() {
        return this.currentArtist;
    }

    public String getCurrentTitle() {
        return this.currentTitle;
    }

    public String getCurrentCoverArtUrl() {
        return this.currentCoverArtUrl;
    }

    /**
     * Searches MusicBrainz database for 'release id' associated with song, then broadcasts cover art URL
     * @param title Title of song
     * @param artist Artist of song
     */
    private void getCoverArtUrl(String title, String artist) {
        // Formats query
        String query = UrlUtils.formatMusicBrainzQuery(title, artist);

        // Create callback object
        Call<RecordingResponse> recordingResponseCall = musicBrainzService.getMBID(query);

        // Enqueue query, handle response
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

    /**
     * Player detects cue point (meta data in stream) containing title/artist of song.
     * Method refreshes fields with this data and broadcasts it.
     * @param mediaPlayer Source of event
     * @param cuePoint Bundle containing title/artist data
     */
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

                    if (!isForeground) {
                        // Start service in foreground
                        startForeground(NOTIFICATION_ID, createNotification());
                        isForeground = true;
                    } else {
                        // Update notification if service already running in foreground
                        mNotificationManager.notify(NOTIFICATION_ID, createNotification());
                    }

                    // Send a broadcast with the new title and artist
                    HashMap<String, String> messageData = new HashMap<>();
                    messageData.put(EXTRA_TITLE, title);
                    messageData.put(EXTRA_ARTIST, artist);
                    sendMessage(INTENT_UPDATE_TITLE_ARTIST, messageData);

                    // Query for the new cover art
                    getCoverArtUrl(title, artist);  // Callback will set currentCoverArtUrl
                }
            }
        }
    }

    /**
     * Callback to adjust sound according to audio focus
     * @param focusChange Integer associated with an action from AudioManager
     */
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
    }

    public class LocalBinder extends Binder {
        RadioPlayerService getService() {
            // Return this instance of the service to expose public methods
            return RadioPlayerService.this;
        }
    }
}
