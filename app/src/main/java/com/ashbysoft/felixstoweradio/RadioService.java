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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

public class RadioService extends Service {
    private static final int NOTIFICATION_PLAYING = 1;
    private MediaPlayer player = null;

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

    private void startNotification() {
        Notification notification = createNotification();
        startForeground(NOTIFICATION_PLAYING, notification);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN) @NonNull private Notification createNotification() {
        Intent notificationIntent = new Intent(this, RadioActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder = new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.status_bar)
                   .setContentTitle(getString(R.string.app_name))
                   .setContentText("Now playing goes here")
                   .setContentIntent(pendingIntent)
                   .setWhen(System.currentTimeMillis())
                   .build();


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
