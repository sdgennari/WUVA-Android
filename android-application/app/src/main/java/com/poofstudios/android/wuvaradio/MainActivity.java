package com.poofstudios.android.wuvaradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver broadcastReceiver;

    Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        stopButton = (Button) findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayer();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(RadioPlayerService.INTENT_UPDATE_COVER_ART)) {
                    String coverArtUrl = intent.getStringExtra(RadioPlayerService.EXTRA_COVER_ART_URL);
                    // TODO Update ui

                } else if (intent.getAction().equals(RadioPlayerService.INTENT_UPDATE_TITLE_ARTIST)) {
                    String title = intent.getStringExtra(RadioPlayerService.EXTRA_TITLE);
                    String artist = intent.getStringExtra(RadioPlayerService.EXTRA_ARTIST);
                    // TODO Update ui

                }
            }
        };

        startPlayer();
    }

    private void stopPlayer() {
        stopService(new Intent(this, RadioPlayerService.class));
    }

    private void startPlayer() {
        Intent intent = new Intent(this, RadioPlayerService.class);
        intent.setAction(RadioPlayerService.ACTION_PLAY);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Register the broadcastReceiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RadioPlayerService.INTENT_UPDATE_COVER_ART);
        intentFilter.addAction(RadioPlayerService.INTENT_UPDATE_TITLE_ARTIST);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        // Unregister the broadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
