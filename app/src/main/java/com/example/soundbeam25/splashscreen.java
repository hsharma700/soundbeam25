package com.example.soundbeam25;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class splashscreen extends AppCompatActivity {

    Handler h= new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar();

        setContentView(R.layout.activity_splashscreen);
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent( splashscreen.this, MusicActivity.class);
                startActivity(i);
                finish();
            }
        },2000);
    }
}