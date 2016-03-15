package com.poofstudios.android.wuvaradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.poofstudios.android.wuvaradio.ui.MainActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class MediaNotificationManager extends BroadcastReceiver {

    // Constants
    public static final int NOTIFICATION_ID = 777;
    public static final int REQUEST_CODE = 100;

    // Actions
    public static final String ACTION_PLAY = "com.poofstudios.android.wuvaradio.play";
    public static final String ACTION_STOP = "com.poofstudios.android.wuvaradio.stop";
    public static final String ACTION_FAVORITE = "com.poofstudios.android.wuvaradio.favorite";
    public static final String ACTION_DISMISS = "com.poofstudios.android.wuvaradio.dismiss";

    // Intents
    private final PendingIntent mPlayIntent;
    private final PendingIntent mStopIntent;
    private final PendingIntent mFavoriteIntent;
    private final PendingIntent mDismissIntent;

    private final RadioPlayerService mService;
    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mMediaController;
    private MediaControllerCompat.TransportControls mTransportControls;
    private PlaybackStateCompat mPlaybackState;
    private MediaMetadataCompat mMediaMetadata;

    private Target mTarget;

    private final NotificationManager mNotificationManager;

    private boolean mStarted = false;

    /**
     * Creates a new MediaNotificationManager and configures the pending intents and session token
     * @param service
     */
    public MediaNotificationManager(RadioPlayerService service) {
        mService = service;
        updateSessionToken();

        mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);

        // Setup PendingIntents
        String pkg = mService.getPackageName();
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mStopIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mFavoriteIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_FAVORITE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mDismissIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_DISMISS).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications
        // Handles case where system killed and restarted RadioPlayerService
        mNotificationManager.cancelAll();
    }

    /**
     * Starts displaying the notification if it can create a valid notification (see
     * {@link #createNotification()}). Puts service in foreground as well to continue playback
     */
    public void startNotification() {
        if (!mStarted) {
            mMediaMetadata = mMediaController.getMetadata();
            mPlaybackState = mMediaController.getPlaybackState();

            Notification notification = createNotification();
            if (notification != null) {
                // Add actions so the BroadcastReceiver knows what to listen for
                mMediaController.registerCallback(mControllerCallback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_STOP);
                filter.addAction(ACTION_FAVORITE);
                filter.addAction(ACTION_DISMISS);
                mService.registerReceiver(this, filter);

                // Move the service to the foreground
                mService.startForeground(NOTIFICATION_ID, notification);
                mStarted = true;
            }
        }
    }

    /**
     * Stops the notification and removes the service from the foreground
     */
    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
            mMediaController.unregisterCallback(mControllerCallback);
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered, so it can be ignored
            }
            mService.stopForeground(true);
        }
    }

    /**
     * Updates the session token and creates a new MediaController
     */
    private void updateSessionToken() {
        MediaSessionCompat.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mMediaController != null) {
                mMediaController.unregisterCallback(mControllerCallback);
            }
            mSessionToken = freshToken;
            try {
                mMediaController = new MediaControllerCompat(mService, mSessionToken);
            } catch (RemoteException e) {
                Log.e("WUVA", e.getLocalizedMessage());
            }
            mTransportControls = mMediaController.getTransportControls();
            if (mStarted) {
                mMediaController.registerCallback(mControllerCallback);
            }
        }
    }

    /**
     * Creates a Notification with actions and metadata from the current session
     * @return new notification with playback controls
     */
    private Notification createNotification() {
        if (mMediaMetadata == null || mPlaybackState == null) {
            return null;
        }
        // Get description from metadata
        MediaDescriptionCompat description = mMediaMetadata.getDescription();

        // Regular remote view
        RemoteViews contentView = new RemoteViews(mService.getPackageName(),
                R.layout.notification_view);
        contentView.setImageViewResource(R.id.image, R.drawable.cover_art_placeholder);
        contentView.setTextViewText(R.id.title, description.getTitle());
        contentView.setTextViewText(R.id.subtitle, description.getSubtitle());

        // Big remote view
//        RemoteViews expandedView = new RemoteViews(mService.getPackageName(),
//                R.layout.notification_big_view);
//        expandedView.setImageViewResource(R.id.image, R.drawable.cover_art_placeholder);
//        expandedView.setTextViewText(R.id.title, description.getTitle());
//        expandedView.setTextViewText(R.id.subtitle, description.getSubtitle());

        // Configure play/stop action
        configurePlayStopButton(contentView);
//        configurePlayStopButton(expandedView);

        // Configure favorite action
        configureFavoriteButton(contentView);
//        configureFavoriteButton(expandedView);

        // Show/hide the favorite button based on playback state
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            contentView.setViewVisibility(R.id.action_favorite, View.VISIBLE);
//            expandedView.setViewVisibility(R.id.action_favorite, View.VISIBLE);
//            expandedView.setViewVisibility(R.id.action_favorite_disabled, View.GONE);
        } else {
            contentView.setViewVisibility(R.id.action_favorite, View.GONE);
//            expandedView.setViewVisibility(R.id.action_favorite, View.GONE);
//            expandedView.setViewVisibility(R.id.action_favorite_disabled, View.VISIBLE);
        }

        // Add data to notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setDeleteIntent(mDismissIntent)
                .setContentIntent(createContentIntent(description));

        // Allow the notification to be dismissed based on state
        setNotificationPlaybackState(builder);

        // Load the image async with Picasso
//        if (description.getIconUri() != null) {
//            loadCoverArtImage(builder, contentView, expandedView,
//                    description.getIconUri().toString());
//        }

        // Set the custom content views for the notification
        Notification notification = builder.build();
        notification.contentView = contentView;
//        notification.bigContentView = expandedView;

        return notification;
    }

    /**
     * Creates a content intent for when the notification is clicked. The intent should load the
     * UI to control the playback
     * @param description description of the current session metadata
     * @return PendingIntent to launch the playback UI
     */
    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        Intent openUI = new Intent(mService, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Include media description if available
        if (description != null) {
            // TODO Add music description as extra
        }
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /**
     * Sets the notification to ongoing if playback is running
     * @param builder builder for the notification
     */
    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        if (mPlaybackState == null || !mStarted || mPlaybackState.getState() == PlaybackStateCompat.STATE_STOPPED) {
            // Playback is not going, so remove the notification
            mService.stopForeground(true);
        } else {
            // Prevent notification from being dismissed if playback is active
            builder.setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING);
        }
    }


    private void configurePlayStopButton(RemoteViews remoteView) {
        int icon;
        PendingIntent pendingIntent;
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_STOPPED) {
            icon = R.drawable.ic_play_arrow_white_24dp;
            pendingIntent = mPlayIntent;
        } else {
            icon = R.drawable.ic_stop_white_24dp;
            pendingIntent = mStopIntent;
        }
        remoteView.setImageViewResource(R.id.action_play_stop, icon);
        remoteView.setOnClickPendingIntent(R.id.action_play_stop, pendingIntent);
    }

    private void configureFavoriteButton(RemoteViews remoteView) {
        int icon = R.drawable.ic_star_border_white_24dp;

        // If the song is already a favorite, change icon
        MediaMetadataCompat metadata = mMediaController.getMetadata();
        if (metadata !=  null) {
            RatingCompat rating = metadata.getRating(MediaMetadataCompat.METADATA_KEY_USER_RATING);
            if (rating != null && rating.hasHeart()) {
                icon = R.drawable.ic_star_white_24dp;
            }
        }
        remoteView.setImageViewResource(R.id.action_favorite, icon);
        remoteView.setOnClickPendingIntent(R.id.action_favorite, mFavoriteIntent);
    }

    private void loadCoverArtImage(final NotificationCompat.Builder builder,
                                   final RemoteViews remoteView, final RemoteViews expandedView,
                                   String coverArtUrl) {
        // Must keep a strong reference to the target to avoid garbage collection
        mTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.d("====", "onBitmapLoaded");
                // Add the image to the remote views
                remoteView.setImageViewBitmap(R.id.image, bitmap);
                expandedView.setImageViewBitmap(R.id.image, bitmap);

                // Update the notification
                Notification notification = builder.build();
                notification.contentView = remoteView;
                notification.bigContentView = expandedView;
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.d("====", "failed");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };
        Picasso.with(mService).load(coverArtUrl).into(mTarget);
    }

    // Extend BroadcastReceiver to handle notification actions
    @Override
    public void onReceive(Context context, Intent intent) {final String action = intent.getAction();
        switch (action) {
            case ACTION_PLAY:
                mTransportControls.play();
                break;
            case ACTION_STOP:
                // Pause will stop the playback but not release the player
                mTransportControls.pause();
                break;
            case ACTION_FAVORITE:
                mTransportControls.sendCustomAction(ACTION_FAVORITE, null);
                break;
            case ACTION_DISMISS:
                // Stop will stop playback, release the player, and stop the service
                mTransportControls.stop();
                break;
            default:
                Log.w("WUVA", "Unknown intent with Action " + action);
        }
    }

    /**
     * Callback for MediaController to update the notification when the session changes
     */
    private final MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            mPlaybackState = playbackState;

            // Update notification based on new PlaybackState
            if (playbackState.getState() == PlaybackStateCompat.STATE_NONE ||
                    playbackState.getState() == PlaybackStateCompat.STATE_ERROR) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    mNotificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMediaMetadata = metadata;

            // Update notification based on new metadata
            Notification notification = createNotification();
            if (notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            updateSessionToken();
        }
    };
}
