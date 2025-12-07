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
import com.example.koscost.utils.SessionManager;

import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    LinearLayout layoutEmail, layoutOtp;
    EditText etEmail, etOtp;
    Button btnRequest, btnVerify;
    TextView tvUlang, tvDaftar; // Variabel tombol daftar
    String userEmail;

    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
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
        tvDaftar = findViewById(R.id.tv_daftar); // Binding

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

        // 4. Tombol Daftar (Pindah Halaman)
        tvDaftar.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void requestOtp(String email) {
        btnRequest.setEnabled(false);
        btnRequest.setText("Mengirim...");

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.requestLoginOtp(email).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnRequest.setEnabled(true);
                btnRequest.setText("Kirim Kode OTP");

                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);

                    if (json.getString("status").equals("sukses")) {
                        Toast.makeText(LoginActivity.this, "OTP Terkirim ke Email!", Toast.LENGTH_SHORT).show();
                        layoutEmail.setVisibility(View.GONE);
                        layoutOtp.setVisibility(View.VISIBLE);
                    } else {
                        // Kalau email belum terdaftar, muncul pesan error dari PHP
                        Toast.makeText(LoginActivity.this, json.getString("message"), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LoginActivity.this, "Gagal koneksi", Toast.LENGTH_SHORT).show();
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
        // Cek email agar tidak null (Pencegahan)
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Email tidak valid, silakan ulangi request OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.verifikasiOtp(email, otp).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res); // Bisa error parsing di sini

                        if (json.optString("status").equals("sukses")) {
                            Toast.makeText(LoginActivity.this, "Login Berhasil!", Toast.LENGTH_SHORT).show();

                            // Simpan Sesi
                            String namaKos = json.optString("nama_kos", "KosCost");
                            sessionManager.createLoginSession(email, namaKos);

                            // Pindah
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Tampilkan pesan gagal dari server (misal "OTP Salah")
                            String msg = json.optString("message", "Verifikasi Gagal");
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // INI YANG SEBELUMNYA HILANG: Menangani Error Server (500, 404, dll)
                        Toast.makeText(LoginActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Tampilkan jika ada error kodingan/parsing
                    Toast.makeText(LoginActivity.this, "Terjadi Kesalahan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Koneksi Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}