package com.poofstudios.android.wuvaradio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.tritondigital.player.TritonPlayer;

public class RadioPlayerService extends Service {

    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final int NOTIFICATION_ID = 777;

    // TODO Get actual Broadcaster and Name from WUVA
    private static final String STATION_BROADCASTER = "WUVA";
    private static final String STATION_NAME = "WUVA";
    private static final String STATION_MOUNT = "WUVA";

    private TritonPlayer mPlayer = null;

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
        mPlayer.play();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
