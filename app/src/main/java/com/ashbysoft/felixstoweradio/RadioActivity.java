package com.ashbysoft.felixstoweradio;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class RadioActivity extends AppCompatActivity {
    private static final String PLAYING = RadioActivity.class.getName() + ".PLAYING";
    private boolean playing = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);

        if (savedInstanceState != null) {
            playing = savedInstanceState.getBoolean(PLAYING);
        }

        if (playing) {
            play();
        }
    }

    @Override protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putBoolean(PLAYING, playing);
    }

    /**
     * Set as onClick for the play button.
     */
    public void onPlayButton(View v) {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    private void pause() {
        stopService(getRadioServiceIntent());
        FloatingActionButton playButton = (FloatingActionButton) findViewById(R.id.play_button);
        playButton.setImageResource(R.drawable.ic_play_arrow_black_48dp);
        playing = false;
    }

    private void play() {
        startService(getRadioServiceIntent());

        FloatingActionButton playButton = (FloatingActionButton)findViewById(R.id.play_button);
        playButton.setImageResource(R.drawable.ic_pause_black_48dp);
        playing = true;
    }

    private boolean isPlaying() {
        return playing;
    }

    private Intent getRadioServiceIntent() {
        return new Intent(this, RadioService.class);
    }
}
