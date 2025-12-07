package com.example.koscost.activities

import android.R
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.koscost.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hilangkan ActionBar di atas (biar full screen)
        if (getSupportActionBar() != null) {
            getSupportActionBar()!!.hide()
        }

        // Delay 2 Detik (2000 ms)
        Handler().postDelayed(Runnable {
            // Pindah ke LoginActivity
            // (Nanti di LoginActivity sudah ada pengecekan sesi, jadi aman)
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }
}