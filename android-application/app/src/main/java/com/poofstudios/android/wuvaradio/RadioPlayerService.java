package com.poofstudios.android.wuvaradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.poofstudios.android.wuvaradio.utils.StringUtils;
import com.tritondigital.player.MediaPlayer;
import com.tritondigital.player.TritonPlayer;

public class RadioPlayerService extends Service implements MediaPlayer.OnCuePointReceivedListener,
        AudioManager.OnAudioFocusChangeListener {

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
    private AudioManager mAudioManager = null;

    public RadioPlayerService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (ACTION_PLAY.equals(action)) {
            // Requesting audio focus will handle creating the player and playing the stream
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startPlayer();

                // Create the system notification
                startPlayerInForeground();
            } else {
                Log.d("====", "AudioFocus Gain Not Granted");
            }
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
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onDestroy() {
        if (mAudioManager != null) {
            int result = mAudioManager.abandonAudioFocus(this);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                stopPlayer();
                releasePlayer();
            } else {
                Log.d("====", "AudioFocus Abandon Not Granted");
            }
        }
        stopForeground(true);
        super.onDestroy();
    }

    private void startPlayerInForeground() {
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // TODO Set default text for player notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setTicker("WUVA Radio");
        builder.setContentTitle("Unknown");
        builder.setContentText("Unknown");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pendingIntent);

        final Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void stopPlayer() {
        if (mPlayer != null && mPlayer.getState() == TritonPlayer.STATE_PLAYING) {
            mPlayer.stop();
        }
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void duckPlayer() {
        if (mPlayer != null && mPlayer.getState() == TritonPlayer.STATE_PLAYING) {
            mPlayer.setVolume(TritonPlayer.VOLUME_DUCK);
        }
    }

    private void startPlayer() {
        if (mPlayer == null || mPlayer.getState() == TritonPlayer.STATE_RELEASED) {
            createPlayer();
        } else if (mPlayer.getState() != TritonPlayer.STATE_PLAYING) {
            mPlayer.play();
        }
        mPlayer.setVolume(TritonPlayer.VOLUME_NORMAL);
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

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d("====", "onAudioFocusChange");
        switch(focusChange) {
            // Gained focus to play music
            case AudioManager.AUDIOFOCUS_GAIN:
                startPlayer();
                break;

            // Lost focus for a long period of time, so release memory
            case AudioManager.AUDIOFOCUS_LOSS:
                stopPlayer();
                releasePlayer();
                break;

            // Lost focus for a short time, will regain soon
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                stopPlayer();
                break;

            // Lost focus for a short time, but can still play quietly
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                duckPlayer();
                break;
        }
    }
}
