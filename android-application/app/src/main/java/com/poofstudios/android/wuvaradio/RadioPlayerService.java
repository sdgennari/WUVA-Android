package com.poofstudios.android.wuvaradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.poofstudios.android.wuvaradio.utils.StringUtils;
import com.tritondigital.player.MediaPlayer;
import com.tritondigital.player.TritonPlayer;

public class RadioPlayerService extends Service implements MediaPlayer.OnCuePointReceivedListener {

    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final int NOTIFICATION_ID = 777;

    private static final String CUE_TITLE = "cue_title";
    private static final String TRACK_ARTIST_NAME = "track_artist_name";

    // TODO Get actual Broadcaster and Name from WUVA
    private static final String STATION_BROADCASTER = "WUVA";
    private static final String STATION_NAME = "WUVA";
    private static final String STATION_MOUNT = "WUVA";

    private TritonPlayer mPlayer = null;
    private NotificationManager mNotificationManager = null;

    public RadioPlayerService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (ACTION_PLAY.equals(action)) {
            createPlayer();
            startPlayerInForeground();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        stopPlayer();
        releasePlayer();
        stopForeground(true);
        super.onDestroy();
    }

    private void startPlayerInForeground() {
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // TODO Set default text for player notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setTicker("Ticker");
        builder.setContentTitle("Content title");
        builder.setContentText("Content text");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pendingIntent);

        final Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void stopPlayer() {
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void createPlayer() {
        // Configure player settings
        Bundle settings = new Bundle();
        settings.putString(TritonPlayer.SETTINGS_STATION_BROADCASTER, STATION_BROADCASTER);
        settings.putString(TritonPlayer.SETTINGS_STATION_NAME, STATION_NAME);
        settings.putString(TritonPlayer.SETTINGS_STATION_MOUNT, STATION_MOUNT);

        // Create and start the player
        mPlayer = new TritonPlayer(this, settings);
        mPlayer.setOnCuePointReceivedListener(this);
        mPlayer.play();
    }

    @Override
    public void onCuePointReceived(MediaPlayer mediaPlayer, Bundle cuePoint) {
        if (mPlayer == mediaPlayer) {
            if (cuePoint != null) {
                String title = StringUtils.capitalizeEveryWord(cuePoint.getString(CUE_TITLE));
                String artist = StringUtils.capitalizeEveryWord(cuePoint.getString(TRACK_ARTIST_NAME));
                if (title != null && artist != null) {
                    final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                            new Intent(getApplicationContext(), MainActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                            .setContentTitle(title)
                            .setContentText(artist)
                            .setTicker(String.format("%s by %s", title, artist))
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentIntent(pendingIntent);
                    mNotificationManager.notify(NOTIFICATION_ID, builder.build());
                }
            }
        }
    }
}
