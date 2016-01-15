package com.poofstudios.android.wuvaradio.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.poofstudios.android.wuvaradio.R;
import com.poofstudios.android.wuvaradio.RadioPlayerService;
import com.poofstudios.android.wuvaradio.utils.BlurTransform;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class RadioFragment extends MediaBaseFragment {

    private LinearLayout mFragmentContent;
    private ImageView mCoverArtView;
    private TextView mTitleView;
    private TextView mArtistView;
    private ToggleButton mStartStopButton;
    private ImageView mBackgroundImage;

    private String mTitle;
    private String mArtist;
    private String mCoverArtUrl;

    public RadioFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_radio, container, false);

        mFragmentContent = (LinearLayout) rootView.findViewById(R.id.fragment_content);
        mCoverArtView = (ImageView) rootView.findViewById(R.id.cover_art);
        mTitleView = (TextView) rootView.findViewById(R.id.title);
        mArtistView = (TextView) rootView.findViewById(R.id.artist);
        mBackgroundImage = (ImageView) rootView.findViewById(R.id.background_image);
        mStartStopButton = (ToggleButton) rootView.findViewById(R.id.start_stop_button);
        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStartStopButton.isChecked()) {
                    startService();
                } else {
                    MediaControllerCompat controller = getActivity().getSupportMediaController();
                    if (controller != null) {
                        controller.getTransportControls().stop();
                    }
                }
            }
        });

        return rootView;
    }

    /**
     * Updates album cover and text with currently playing song
     */
    private void updateUI() {
        mArtistView.setText(mArtist);
        mTitleView.setText(mTitle);

        Context context = getActivity();
        if(context != null && mCoverArtUrl != null && !mCoverArtUrl.isEmpty()) {
            Picasso.with(context).load(mCoverArtUrl).placeholder(R.drawable.cover_art_placeholder).fit().centerInside().into(mCoverArtView);
            Picasso.with(context).load(mCoverArtUrl).transform(new BlurTransform(context)).into(mBackgroundImage);
        } else {
            Picasso.with(context).load(R.drawable.cover_art_placeholder).fit().centerInside().into(mCoverArtView);
            mBackgroundImage.setImageResource(R.color.darkColorPrimaryDark);
        }

        Log.d("url", "Cover art: " + mCoverArtUrl);
    }


    private void startService() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), RadioPlayerService.class);
            intent.setAction(RadioPlayerService.CMD_PLAY);
            getActivity().startService(intent);
        }
    }

    @Override
    protected void updatePlaybackState(PlaybackStateCompat playbackState) {
        if (playbackState == null) {
            return;
        }

        // Update the UI based on the current playback state
        switch(playbackState.getState()) {
            case PlaybackStateCompat.STATE_CONNECTING:
                mArtistView.setText("Connecting...");
                mTitleView.setText("");
                mStartStopButton.setChecked(true);
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                updateUI();
                mStartStopButton.setChecked(true);
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                mArtistView.setText("Playback stopped.");
                mTitleView.setText("");
                mStartStopButton.setChecked(false);
                break;
            default:
                Log.d("WUVA", "Unhandled state " + playbackState.getState());
        }


        // Can handle other button visibility with playbackState.getActions() or
        // playbackState.getCustomActions()
    }

    @Override
    protected void updateMediaDescription(MediaDescriptionCompat description) {
        Log.d("====", "updateMediaDescription");
        if (description == null) {
            return;
        }

        // Update the variables
        mTitle = description.getTitle().toString();
        mArtist = description.getSubtitle().toString();

        if (description.getIconUri() != null) {
            mCoverArtUrl = description.getIconUri().toString();
        } else {
            // Request is still pending, so no MediaUri set
            mCoverArtUrl = "";
        }

        updateUI();
    }
}
