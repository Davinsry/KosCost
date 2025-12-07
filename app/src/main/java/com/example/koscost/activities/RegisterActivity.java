package com.example.koscost.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etNamaKos;
    Button btnDaftar;
    TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.et_email_reg);
        etNamaKos = findViewById(R.id.et_nama_kos_reg);
        btnDaftar = findViewById(R.id.btn_daftar_akun);
        tvLogin = findViewById(R.id.tv_login_link);

        btnDaftar.setOnClickListener(v -> prosesDaftar());

        // Klik text login -> kembali (tutup activity ini)
        tvLogin.setOnClickListener(v -> finish());
    }

    private void prosesDaftar() {
        String email = etEmail.getText().toString().trim();
        String namaKos = etNamaKos.getText().toString().trim();

        if (email.isEmpty() || namaKos.isEmpty()) {
            Toast.makeText(this, "Semua data wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.registerUser(email, namaKos).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        if (json.getString("status").equals("sukses")) {
                            Toast.makeText(RegisterActivity.this, "Pendaftaran Berhasil! Silakan Login.", Toast.LENGTH_LONG).show();
                            finish(); // Kembali ke halaman Login
                        } else {
                            Toast.makeText(RegisterActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Gagal terhubung server", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Koneksi Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}