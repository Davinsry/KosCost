package com.example.koscost.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.koscost.MainActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;

import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.koscost.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    LinearLayout layoutEmail, layoutOtp;
    EditText etEmail, etOtp;
    Button btnRequest, btnVerify;
    TextView tvUlang;
    String userEmail; // Simpan email sementara
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Inisialisasi & Cek Sesi (Ditaruh PALING ATAS sebelum setContentView)
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            // Kalau sudah login, langsung ke Dashboard
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return; // Stop kode di bawahnya
        }

        setContentView(R.layout.activity_login);

        // Binding
        layoutEmail = findViewById(R.id.layout_email);
        layoutOtp = findViewById(R.id.layout_otp);
        etEmail = findViewById(R.id.et_email);
        etOtp = findViewById(R.id.et_otp);
        btnRequest = findViewById(R.id.btn_request_otp);
        btnVerify = findViewById(R.id.btn_verify_otp);
        tvUlang = findViewById(R.id.tv_ulang_email);

        // 1. Tombol Kirim OTP
        btnRequest.setOnClickListener(v -> {
            userEmail = etEmail.getText().toString().trim();
            if (!userEmail.isEmpty()) {
                requestOtp(userEmail);
            } else {
                Toast.makeText(this, "Email wajib diisi!", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Tombol Verifikasi OTP
        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (!otp.isEmpty()) {
                verifyOtp(userEmail, otp);
            } else {
                Toast.makeText(this, "OTP wajib diisi!", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Tombol Ulang
        tvUlang.setOnClickListener(v -> {
            layoutOtp.setVisibility(View.GONE);
            layoutEmail.setVisibility(View.VISIBLE);
        });
    }

    private void requestOtp(String email) {
        // Tampilkan loading (opsional)
        btnRequest.setEnabled(false);
        btnRequest.setText("Mengirim...");

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.requestLoginOtp(email).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnRequest.setEnabled(true);
                btnRequest.setText("Kirim Kode OTP");

                if (response.isSuccessful()) {
                    // Pindah Tampilan ke Input OTP
                    Toast.makeText(LoginActivity.this, "OTP Terkirim ke Email!", Toast.LENGTH_SHORT).show();
                    layoutEmail.setVisibility(View.GONE);
                    layoutOtp.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(LoginActivity.this, "Gagal kirim OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnRequest.setEnabled(true);
                btnRequest.setText("Kirim Kode OTP");
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp(String email, String otp) {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.verifikasiOtp(email, otp).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        if (json.getString("status").equals("sukses")) {
                            Toast.makeText(LoginActivity.this, "Login Berhasil!", Toast.LENGTH_SHORT).show();

                            // --- TAMBAHKAN INI: Simpan Sesi ---
                            String namaKos = json.optString("nama_kos", "KosCost"); // Default fallback
                            sessionManager.createLoginSession(email, namaKos);
                            // ----------------------------------

                            // Pindah ke Dashboard Utama
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "OTP Salah!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}