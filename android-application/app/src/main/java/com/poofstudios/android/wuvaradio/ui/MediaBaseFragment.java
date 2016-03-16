package com.poofstudios.android.wuvaradio.ui;

import android.support.v4.app.Fragment;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.poofstudios.android.wuvaradio.MediaNotificationManager;
import com.poofstudios.android.wuvaradio.model.Favorite;
import com.poofstudios.android.wuvaradio.model.Track;

public abstract class MediaBaseFragment extends Fragment {

    private final MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            MediaBaseFragment.this.updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                MediaBaseFragment.this.updateMediaDescription(metadata.getDescription());
            }
        }
    };

    public void onStart() {
        super.onStart();
        if (getActivity() != null && getActivity().getSupportMediaController() != null) {
            onControllerConnected();
        }
    }

    public void onControllerConnected() {
        MediaControllerCompat controller = getActivity().getSupportMediaController();
        if (controller != null) {
            controller.registerCallback(mControllerCallback);
            if (controller.getPlaybackState() != null) {
                updatePlaybackState(controller.getPlaybackState());
            }
            if (controller.getMetadata() != null) {
                updateMediaDescription(controller.getMetadata().getDescription());
            }
        }
    }

    public boolean maybeUpdateCurrentSongFavorite(Favorite favorite) {
        MediaControllerCompat controller = getActivity().getSupportMediaController();
        if (controller != null) {
            MediaMetadataCompat metadata = controller.getMetadata();
            if (metadata != null && metadata.getDescription() != null) {
                MediaDescriptionCompat description = metadata.getDescription();

                // Compare title and artist of favorite to those of the currently playing song
                Track favoriteTrack = favorite.getTrack();
                if (favoriteTrack.getTitle().equals(description.getTitle()) &&
                        favoriteTrack.getArtist().equals(description.getSubtitle())) {
                    // Notify that the current metadata has changed
                    controller.getTransportControls().sendCustomAction(
                            MediaNotificationManager.ACTION_FAVORITE, null);
                    return true;
                }
            }
        }
        return false;
    }

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
