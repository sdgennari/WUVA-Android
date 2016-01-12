package com.poofstudios.android.wuvaradio.ui;

import android.support.v4.app.Fragment;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

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
