package com.cf.supervideodemo;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.cf.supervideolibrary.SuperPlayer;

public class MainActivity extends AppCompatActivity implements SuperPlayer.OnNetChangeListener {

    private SuperPlayer mPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        String url = "http://192.168.1.105:8080/video/RolyPoly.mp4";

        mPlay = (SuperPlayer) findViewById(R.id.view_super_player);

        mPlay.setNetChangeListener(true)
                .setOnNetChangeListener(this)
                .showCenterControl(true)
                .setTitle("T-ara")
                .play(url);
    }

    @Override
    public void onWifi() {
        Toast.makeText(MainActivity.this, "onWifi", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onMobile() {
        Toast.makeText(MainActivity.this, "onMobile", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisConnect() {
        Toast.makeText(MainActivity.this, "onDisConnect", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onNoAvailable() {
        Toast.makeText(MainActivity.this, "onNoAvailable", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlay != null) {
            mPlay.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPlay != null) {
            mPlay.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlay != null) {
            mPlay.onDestroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (mPlay != null && mPlay.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mPlay != null) {
            mPlay.onConfigurationChanged(newConfig);
        }
    }
}
