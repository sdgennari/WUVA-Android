package com.poofstudios.android.wuvaradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    // Intents
    private final PendingIntent mPlayIntent;
    private final PendingIntent mStopIntent;
    private final PendingIntent mFavoriteIntent;

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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService);

        // Add play/stop action
//        addPlayStopButton(builder);

        // Add favorite button
//        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
//            addFavoriteButton(builder);
//        }

        // Get description from metadata
        MediaDescriptionCompat description = mMediaMetadata.getDescription();

        // Get placeholder bitmap
        Bitmap placeholderBmp = BitmapFactory.decodeResource(mService.getResources(), R.drawable.cover_art_placeholder);

        // Regular remote view
        RemoteViews contentView = new RemoteViews(mService.getPackageName(),
                R.layout.notification_view);
        contentView.setImageViewResource(R.id.image, R.drawable.cover_art_placeholder);
        contentView.setTextViewText(R.id.title, description.getTitle());
        contentView.setTextViewText(R.id.subtitle, description.getSubtitle());

        // Big remote view
        RemoteViews expandedView = new RemoteViews(mService.getPackageName(),
                R.layout.notification_big_view);
        expandedView.setImageViewResource(R.id.image, R.drawable.cover_art_placeholder);
        expandedView.setTextViewText(R.id.title, description.getTitle());
        expandedView.setTextViewText(R.id.subtitle, description.getSubtitle());

        // Add data to notification
        builder.setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle(description.getTitle())
//                .setContentText(description.getSubtitle())
//                .setLargeIcon(placeholderBmp)           // Always set this to overwrite old image
//                .setStyle(new NotificationCompat.MediaStyle()
//                        .setMediaSession(mSessionToken))
                .setContentIntent(createContentIntent(description));

        // Allow the notification to be dismissed based on state
//        setNotificationPlaybackState(builder);

        // Load the image async with Picasso
//        if (description.getIconUri() != null) {
//            loadCoverArtImage(builder, description.getIconUri().toString());
//        }

        Notification notification = builder.build();
        notification.contentView = contentView;
        notification.bigContentView = expandedView;

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
        if (mPlaybackState == null || !mStarted) {
            // Playback is not going, so remove the notification
            mService.stopForeground(true);
        } else {
            // Prevent notification from being dismissed if playback is active
            builder.setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING);
        }
    }

    /**
     * Adds either a play or a stop button depending on playback state
     * @param builder builder for the notification
     */
    private void addPlayStopButton(NotificationCompat.Builder builder) {
        String label;
        int icon;
        PendingIntent pendingIntent;
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_STOPPED) {
            label = mService.getString(R.string.action_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
            pendingIntent = mPlayIntent;
        } else {
            label = mService.getString(R.string.action_stop);
            icon = R.drawable.ic_stop_white_24dp;
            pendingIntent = mStopIntent;
        }
        builder.addAction(icon, label, pendingIntent);
    }

    /**
     * Adds a favorite button and changes icon if the song is already marked as favorite
     * @param builder builder for the notification
     */
    private void addFavoriteButton(NotificationCompat.Builder builder) {
        int icon = R.drawable.ic_star_border_white_24dp;

        // If the song is already a favorite, change icon
        MediaMetadataCompat metadata = mMediaController.getMetadata();
        if (metadata !=  null) {
            RatingCompat rating = metadata.getRating(MediaMetadataCompat.METADATA_KEY_USER_RATING);
            if (rating != null && rating.hasHeart()) {
                icon = R.drawable.ic_star_white_24dp;
            }
        }
        builder.addAction(icon, mService.getString(R.string.action_favorite), mFavoriteIntent);
    }

    private void loadCoverArtImage(final NotificationCompat.Builder builder, String coverArtUrl) {
        // Must keep a strong reference to the target to avoid garbage collection
        mTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // Add the image to the notification and update it
                builder.setLargeIcon(bitmap);
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
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
                mTransportControls.stop();
                break;
            case ACTION_FAVORITE:
                mTransportControls.sendCustomAction(ACTION_FAVORITE, null);
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
            if (playbackState.getState() == PlaybackStateCompat.STATE_STOPPED ||
                    playbackState.getState() == PlaybackStateCompat.STATE_NONE) {
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
