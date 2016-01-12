package com.poofstudios.android.wuvaradio.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.poofstudios.android.wuvaradio.R;
import com.squareup.picasso.Picasso;

public class PlaybackControlsFragment extends Fragment {

    private ImageView mCoverArtView;
    private ImageButton mPlayStopButton;
    private TextView mTitleView;
    private TextView mArtistView;

    private final MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            PlaybackControlsFragment.this.updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                PlaybackControlsFragment.this.updateMediaDescription(metadata.getDescription());
            }
        }
    };

    public PlaybackControlsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        // Configure views
        mCoverArtView = (ImageView) rootView.findViewById(R.id.cover_art_image);
        mTitleView = (TextView) rootView.findViewById(R.id.title);
        mArtistView = (TextView) rootView.findViewById(R.id.artist);
        mPlayStopButton = (ImageButton) rootView.findViewById(R.id.button_play_stop);
        mPlayStopButton.setOnClickListener(mPlayStopListener);

        return rootView;
    }

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

    private void updatePlaybackState(PlaybackStateCompat playbackState) {
        if (playbackState == null) {
            return;
        }

        // Update the image on the play/stop button
        int icon = R.drawable.ic_stop_black_36dp;
        if (playbackState.getState() == PlaybackStateCompat.STATE_STOPPED) {
            icon = R.drawable.ic_play_arrow_black_36dp;
        }
        mPlayStopButton.setImageResource(icon);
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }

        // Update the title and artist
        mTitleView.setText(description.getTitle());
        mArtistView.setText(description.getSubtitle());

        // Update the cover art if the url is available
        if (description.getIconUri() != null) {
            String coverArtImageUrl = description.getIconUri().toString();
            Picasso.with(getActivity()).load(coverArtImageUrl)
                    .placeholder(R.drawable.cover_art_placeholder)
                    .error(R.drawable.cover_art_placeholder)
                    .into(mCoverArtView);
        } else {
            Picasso.with(getActivity()).load(R.drawable.cover_art_placeholder).into(mCoverArtView);
        }
    }

    private void startPlayback() {
        MediaControllerCompat controller = getActivity().getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void stopPlayback() {
        MediaControllerCompat controller = getActivity().getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().stop();
        }
    }

    private final View.OnClickListener mPlayStopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaControllerCompat controller = getActivity().getSupportMediaController();
            if (controller == null) {
                return;
            }

            PlaybackStateCompat playbackState = controller.getPlaybackState();
            int state = playbackState.getState();
            switch (state) {
                case PlaybackStateCompat.STATE_BUFFERING:
                case PlaybackStateCompat.STATE_CONNECTING:
                case PlaybackStateCompat.STATE_PLAYING:
                    stopPlayback();
                    break;
                case PlaybackStateCompat.STATE_NONE:
                case PlaybackStateCompat.STATE_STOPPED:
                    startPlayback();
                    break;
                default:
                    Log.d("WUVA", "Unhandled playback state in PlaybackControlsFragment");
            }

        }
    };
}
