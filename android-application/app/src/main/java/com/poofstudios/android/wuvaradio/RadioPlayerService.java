package com.poofstudios.android.wuvaradio;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.poofstudios.android.wuvaradio.api.MusicBrainzApi;
import com.poofstudios.android.wuvaradio.api.MusicBrainzService;
import com.poofstudios.android.wuvaradio.api.model.RecordingResponse;
import com.poofstudios.android.wuvaradio.model.Favorite;
import com.poofstudios.android.wuvaradio.model.Track;
import com.poofstudios.android.wuvaradio.utils.UrlUtils;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class RadioPlayerService extends Service implements
        AudioManager.OnAudioFocusChangeListener,
        RadioPlayback.Callback {

    public static final String CMD_PLAY = "CMD_PLAY";

    // Binds activity to service
    private final IBinder mBinder = new LocalBinder();

    // Handles audio focus (phone, GPS, etc.)
    private AudioManager mAudioManager;

    // For getting album cover art
    private MusicBrainzService musicBrainzService;

    // Manages notifications
    private MediaNotificationManager mMediaNotificationManager;

    // Manages user favorites
    private FavoriteManager mFavoriteManager;

    // Establishes radio connection
    private RadioPlayback mPlayback;

    // Media Sessions
    private MediaSessionCompat mMediaSession = null;
    private static final String MEDIA_SESSION_TAG = "WUVAMediaSession";
    private static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PLAY |
            PlaybackStateCompat.ACTION_STOP;

    private static final boolean ALLOW_REBIND = true;

    /**
     * Initializes managers, MusicBrainz service
     */
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
        mFavoriteManager = FavoriteManager.getFavoriteManager(this);
    }

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

        // Play radio if play action
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
      * Stops player, releases media player instance
     */
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

    /**
     * Searches MusicBrainz database for 'release id' associated with song, then broadcasts cover art URL
     * @param title Title of song
     * @param artist Artist of song
     */
    private void getCoverArtUrl(String title, String artist) {
        Log.d("====", "getCoverArtUrl");
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
                            addCoverArtUrlToMetadata(UrlUtils.formatCoverArtUrl(release.id));
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
     * Adds the coverArtUrl to the current session metadata
     * @param coverArtUrl url for the cover art image
     */
    private void addCoverArtUrlToMetadata(String coverArtUrl) {
        Log.d("====", "addCoverArtUrlToMetadata");

        // Transfer the old metadata to a new metadata object
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder(
                mMediaSession.getController().getMetadata());

        // Add the new coverArtUrl to the metadata
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, coverArtUrl);

        // Update the metadata for the session
        mMediaSession.setMetadata(metadataBuilder.build());
    }

    /**
     * Adds a rating to the current session metadata
     * @param rating rating of the current song
     */
    private void addRatingToMetadata(RatingCompat rating) {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder(
                mMediaSession.getController().getMetadata());

        // Add the rating to the metadata
        metadataBuilder.putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING, rating);

        // Update the metadata for the session
        mMediaSession.setMetadata(metadataBuilder.build());
    }

    /**
     * Updates the session metadata and queries for the cover art url. Metadata deals with the info
     * about the song that is currently playing (title, artist, etc.)
     * @param metadata new metadata for the session
     */
    private void updateMediaSessionMetadata(MediaMetadataCompat metadata) {
        Log.d("====", "updateMediaSessionMetadata");
        // Update the metadata for the session
        mMediaSession.setMetadata(metadata);

        MediaDescriptionCompat description = metadata.getDescription();
        if (description != null) {
            String title = String.valueOf(description.getTitle());
            String artist = String.valueOf(description.getSubtitle());

            // Check if the song is a favorite
            Track track = new Track(title, artist);
            boolean isFavorite = mFavoriteManager.isFavorite(new Favorite(track));

            // Add the rating to the metadata
            RatingCompat rating = RatingCompat.newHeartRating(isFavorite);
            addRatingToMetadata(rating);


            // Make an async query to get the cover art url
            getCoverArtUrl(title, artist);
        }
    }

    /**
     * Updates the session playback state and creates a notification if necessary. PlaybackState
     * deals with the controls available to the user (play, stop, favorite, etc.)
     */
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

    /**
     * Callback to adjust sound according to audio focus
     * @param focusChange Integer associated with an action from AudioManager
     */
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
        public RadioPlayerService getService() {
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

        /**
         * Starts playback
         */
        @Override
        public void onPlay() {
            mPlayback.play();
        }

        /**
         * Stops playback and stops the service
         */
        @Override
        public void onStop() {
            Log.d("====", "MediaSessionCallback onStop");
            mPlayback.stop();
            mPlayback.release();

            // Playback ended, so stop the service
            RadioPlayerService.this.stopSelf();
        }

        /**
         * Handles custom actions send to the media session.
         * ACTION_FAVORITE updates a track in the user's favorite
         * @param action action sent in the custom action call
         * @param extras optional extras
         */
        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            if (MediaNotificationManager.ACTION_FAVORITE.equals(action)) {
                MediaMetadataCompat metadata = mMediaSession.getController().getMetadata();
                if (metadata != null && metadata.getDescription() != null) {
                    MediaDescriptionCompat description = metadata.getDescription();

                    // Create a favorite object from the current song
                    Track track = new Track(description.getTitle(), description.getSubtitle());
                    Favorite favorite = new Favorite(track);

                    // If not already a favorite, then added to favorites
                    // Otherwise is removed from favorites
                    boolean isFavorite = !mFavoriteManager.isFavorite(favorite);

                    // Update the favorite in the user's favorite list
                    mFavoriteManager.setFavorite(favorite, isFavorite);

                    // Update the session metadata
                    RatingCompat rating = RatingCompat.newHeartRating(isFavorite);
                    addRatingToMetadata(rating);
                }
            }
        }
    }
}
