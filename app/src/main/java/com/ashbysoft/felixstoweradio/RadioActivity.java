package com.ashbysoft.felixstoweradio;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class RadioActivity extends AppCompatActivity {
    // For testing
    //private static final int NOW_PLAYING_REFRESH_MILLIS = 5 * 60 * 1000;
    //private static final int NOW_PLAYING_REFRESH_MILLIS = 10 * 1000;
    private static final String PLAYING = RadioActivity.class.getName() + ".PLAYING";
    private boolean playing = false;

    // Static so they don't get destroyed with the activity, and the service can use them.
    private static String nowPlaying = "";
    private static String nextPlaying = "";

    // For google calendar to access schedule
    private static final int REQUEST_ACCOUNT_PICKER = 1000;
    private static final int REQUEST_AUTHORIZATION = 1001;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };

    // Used by the RadioService, outside the lifecycle of the activity.
    private static com.google.api.services.calendar.Calendar mService;

    private final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    private final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    private GoogleAccountCredential credential;

    /**
     * AppCompatActivity
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);

        initCalendarService();

        if (savedInstanceState != null) {
            playing = savedInstanceState.getBoolean(PLAYING);
            updateTextViews();
        }

        if (playing) {
            play();
        }

        refreshNowPlaying();
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

    private void initCalendarService() {
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));

        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(getString(R.string.app_name))
                .build();
    }

    private void refreshNowPlaying() {
        //scheduleNextRefresh();
        if (isGooglePlayServicesAvailable()) {
            if (credential.getSelectedAccountName() == null) {
                chooseAccount();
            } else {
                if (isDeviceOnline()) {
                    new ApiAsyncTask().execute();
                } else {
                    updateNowPlaying(new Pair<>("No network connection available.", ""));
                }
            }
        } else {
            updateNowPlaying(new Pair<>("Google Play Services required: " +
                    "after installing, close and relaunch this app.", ""));
        }
    }

    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    @Override protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    updateNowPlaying(new Pair<>("Account unspecified.", ""));
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    private void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode,
                        RadioActivity.this,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    private void updateNowPlaying(final Pair<String, String> nowAndNext) {
        nowPlaying = nowAndNext.first;
        nextPlaying = nowAndNext.second;
        updateTextViews();
    }

    private void updateTextViews() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                TextView nowPlayingTextView = (TextView) findViewById(R.id.now_playing);
                TextView nextPlayingTextView = (TextView) findViewById(R.id.next_playing);
                nowPlayingTextView.setText(nowPlaying);
                nextPlayingTextView.setText(nextPlaying);
            }
        });
    }

    private class ApiAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                updateNowPlayingAndNextFromApi();
                updateTextViews();
            } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                showGooglePlayServicesAvailabilityErrorDialog(
                        availabilityException.getConnectionStatusCode());

            } catch (UserRecoverableAuthIOException userRecoverableException) {
                startActivityForResult(
                        userRecoverableException.getIntent(),
                        RadioActivity.REQUEST_AUTHORIZATION);

            } catch (Exception e) {
                updateNowPlaying(new Pair<>("The following error occurred", e.getMessage()));
            }
            return null;
        }

    }

    /**
     * Called both from this activity, and from the RadioService.
     * Updates the nowPlaying and nextPlaying variables.
     */
    public static void updateNowPlayingAndNextFromApi() throws IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = mService.events().list("fxrdeanweb@gmail.com")
                .setMaxResults(2)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Event> items = events.getItems();
        nowPlaying = getEventSummary(items.get(0));
        nextPlaying = getEventSummaryAndTime(items.get(1));
    }

    private static String getEventSummary(Event evt) {
        if (evt == null) {
            return "Nothing!";
        } else {
            return evt.getSummary();
        }
    }

    private static String getEventSummaryAndTime(Event evt) {
        if (evt == null) {
            return "Nothing!";
        } else {
            EventDateTime eventDateTime = evt.getStart();
            DateTime dt = eventDateTime.getDateTime();
            Date d = new Date(dt.getValue());
            String evtTime = SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(d);
            return String.format("%s (%s)", evt.getSummary(), evtTime);
        }
    }

    public static String getNowPlaying() {
        return nowPlaying;
    }

    public static void setNowPlaying(String nowPlaying) {
        RadioActivity.nowPlaying = nowPlaying;
    }

    public static String getNextPlaying() {
        return nextPlaying;
    }

    public static void setNextPlaying(String nextPlaying) {
        RadioActivity.nextPlaying = nextPlaying;
    }
}
