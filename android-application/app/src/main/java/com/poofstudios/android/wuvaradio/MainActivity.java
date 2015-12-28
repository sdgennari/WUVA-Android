package com.poofstudios.android.wuvaradio;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.tritondigital.player.TritonPlayer;

public class MainActivity extends AppCompatActivity {

    TritonPlayer player;

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

        testInitPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.release();
        }
    }

    // TODO Remove this method from the activity and put in a service
    private void testInitPlayer() {
        Bundle settings = new Bundle();
        settings.putString(TritonPlayer.SETTINGS_STATION_BROADCASTER, "WUVA");
        settings.putString(TritonPlayer.SETTINGS_STATION_NAME, "WUVA");
        settings.putString(TritonPlayer.SETTINGS_STATION_MOUNT, "WUVA");

        player = new TritonPlayer(this, settings);
        player.play();
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
