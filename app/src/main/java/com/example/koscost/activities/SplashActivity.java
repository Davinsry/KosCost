package com.example.koscost.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R; // Pastikan ini mengarah ke R project kamu

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hilangkan ActionBar di atas (biar full screen)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Delay 2 Detik (2000 ms)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Pindah ke LoginActivity
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Tutup Splash agar user tidak bisa back ke sini
            }
        }, 2000);
    }
}