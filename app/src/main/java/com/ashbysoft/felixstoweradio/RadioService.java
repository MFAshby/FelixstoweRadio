package com.ashbysoft.felixstoweradio;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

/**
 * Background radio player
 */
public class RadioService extends Service {
    private static final int NOTIFICATION_PLAYING = 1;
    private MediaPlayer player = null;

    /**
     * Service - not bindable
     */
    @Nullable @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public int onStartCommand(Intent i, int flags, int startId) {
        if (player == null || !player.isPlaying()) {
            start();
        }

        return START_STICKY_COMPATIBILITY;
    }

    @Override public void onDestroy() {
        stop();
    }

    private void start() {
        stopAndReleasePlayer();
        startPlayer();
        startNotification();
    }

    private void stop() {
        stopForeground(true);
        stopAndReleasePlayer();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN) private void startNotification() {
        // Also prep notification
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        Intent i = new Intent(getApplicationContext(), RadioActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = builder.setContentTitle("Felixstowe Radio")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_PLAYING, notification);
    }

    private void startPlayer() {
        try {
            player = new MediaPlayer();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    player.start();
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("", "Error loading media player: what=" + what + " extra=" + extra);
                    return false;
                }
            });
            player.setDataSource(getApplicationContext(), Uri.parse("http://uk4-vn.mixstream.net/8032.m"));
            player.prepareAsync();
        } catch (IOException ioe) {
            Log.e("", "Error loading radio", ioe);
        }
    }

    private void stopAndReleasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }
}
