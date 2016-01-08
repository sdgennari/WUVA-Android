package com.poofstudios.android.wuvaradio;

import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.poofstudios.android.wuvaradio.utils.StringUtils;
import com.tritondigital.player.MediaPlayer;
import com.tritondigital.player.TritonPlayer;

/**
 * Wrapper class for TritonPlayer to handle player creation and callbacks. The RadioPlayerService
 * holds an instance of RadioPlayback to control radio playback.
 */
public class RadioPlayback implements MediaPlayer.OnCuePointReceivedListener,
        MediaPlayer.OnStateChangedListener{

    // TritonPlayer metadata fields
    private static final String CUE_TITLE = "cue_title";
    private static final String TRACK_ARTIST_NAME = "track_artist_name";

    // TODO Get actual Broadcaster and Name from WUVA
    private static final String STATION_BROADCASTER = "WUVA";
    private static final String STATION_NAME = "WUVA";
    private static final String STATION_MOUNT = "WUVA";

    // mState is one of the states given by PlaybackStateCompat.STATE_*
    private int mState;

    // Instance of the TritonPlayer to play the radio
    private TritonPlayer mPlayer;

    // Instance of the Callback interface to handle playback updates
    private Callback mCallback;

    private RadioPlayerService mService;

    // Last title and artist received in metadata
    private String mLastTitle;
    private String mLastArtist;

    public RadioPlayback(RadioPlayerService service) {
        this.mService = service;
    }

    /**
     * Starts radio playback
     */
    public void play() {
        // Create a new player if needed
        if (mPlayer == null || mPlayer.getState() == TritonPlayer.STATE_RELEASED) {
            createPlayer();
        }

        // Start playback if it is not already running
        if (mState != PlaybackStateCompat.STATE_PLAYING) {
            mPlayer.play();
        }
        mPlayer.setVolume(TritonPlayer.VOLUME_NORMAL);
    }

    /**
     * Stops the radio playback
     */
    public void stop() {
        if (mPlayer != null && mState == PlaybackStateCompat.STATE_PLAYING) {
            mPlayer.stop();
        }
    }

    /**
     * Ducks the radio playback (sets the volume to 0.2f)
     */
    public void duck() {
        if (mPlayer != null && mState == PlaybackStateCompat.STATE_PLAYING) {
            mPlayer.setVolume(TritonPlayer.VOLUME_DUCK);
        }
    }

    /**
     * Releases the radio playback and frees memory
     */
    public void release() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    /**
     * Creates the TritonPlayer with the appropriate station settings
     */
    private void createPlayer() {
        // Configure player settings
        Bundle settings = new Bundle();
        settings.putString(TritonPlayer.SETTINGS_STATION_BROADCASTER, STATION_BROADCASTER);
        settings.putString(TritonPlayer.SETTINGS_STATION_NAME, STATION_NAME);
        settings.putString(TritonPlayer.SETTINGS_STATION_MOUNT, STATION_MOUNT);

        // Release the old player if it exists
        if (mPlayer != null) {
            mPlayer.release();
        }

        // Create a new player
        mPlayer = new TritonPlayer(mService, settings);
        mPlayer.setOnCuePointReceivedListener(this);
        mPlayer.setOnStateChangedListener(this);
    }

    /**
     * Player detects cue point (meta data in stream) containing title/artist of song.
     * Method refreshes fields with this data and notifies the callback
     * @param mediaPlayer Source of event
     * @param cuePoint Bundle containing title/artist data
     */
    @Override
    public void onCuePointReceived(MediaPlayer mediaPlayer, Bundle cuePoint) {
        if (mPlayer == mediaPlayer) {
            if (cuePoint != null) {
                String title = StringUtils.capitalizeEveryWord(cuePoint.getString(CUE_TITLE));
                String artist = StringUtils.capitalizeEveryWord(cuePoint.getString(TRACK_ARTIST_NAME));

                // Check for valid and non-repeated metadata
                if (title != null && artist != null && !title.equals(mLastTitle) && !artist.equals(mLastArtist)) {
                    // Update last title and artist
                    mLastTitle = title;
                    mLastArtist = artist;

                    // Create a MediaMetadata with the new values
                    MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title);
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist);
                    // Note: The album art url is not available as metadata from the radio stream
                    // Instead, it must be fetched from the MusicBrainzApi (done in the service)

                    // Notify the callback that the metadata has changed
                    if (mCallback != null) {
                        mCallback.onMetadataChanged(metadataBuilder.build());
                    }
                }
            }
        }
    }

    /**
     * Callback when TritonPlay changes state. Converts the TritonPlayer state to mState and
     * notifies the callback
     * @param mediaPlayer Source of event
     * @param state New state of the player
     */
    @Override
    public void onStateChanged(MediaPlayer mediaPlayer, int state) {
        if (mPlayer == mediaPlayer) {
            // Update the value of mState based on MediaPlayer state
            switch (state) {
                case TritonPlayer.STATE_CONNECTING:
                    mState = PlaybackStateCompat.STATE_CONNECTING;
                    break;
                case TritonPlayer.STATE_PLAYING:
                    mState = PlaybackStateCompat.STATE_PLAYING;
                    break;
                case TritonPlayer.STATE_STOPPED:
                    mState = PlaybackStateCompat.STATE_STOPPED;
                    break;
                case TritonPlayer.STATE_RELEASED:   // fallthrough
                default:
                    mState = PlaybackStateCompat.STATE_NONE;
            }
            // Notify the callback that the state has changed
            if (mCallback != null) {
                mCallback.onPlaybackStateChanged();
            }
        }
    }

    /**
     * Sets the state of the playback
     * @param state state of the playback (a state in PlaybackStateCompat.STATE_*)
     */
    public void setState(int state) {
        this.mState = state;
    }

    /**
     * Gets the state of the playback
     * @return state of the playback (a state in PlaybackStateCompat.STATE_*)
     */
    public int getState() {
        return mState;
    }

    /**
     * Sets the callback to receive events from the RadioPlayback
     * @param callback callback to receive events
     */
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    /**
     * Interface to make callbacks from the radio playback to the service. The service can then
     * update the MediaSession with the appropriate information
     */
    public interface Callback {

        /**
         * Called when the playback state has changed
         * Used to update notifications, ui, service life-cycle, etc.
         */
        void onPlaybackStateChanged();

        /**
         * Called when new metadata is available from the radio stream
         */
        void onMetadataChanged(MediaMetadataCompat metadata);

    }
}
