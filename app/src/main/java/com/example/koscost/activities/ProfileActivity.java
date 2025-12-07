package com.example.koscost.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.utils.SessionManager;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    EditText etEmail, etNamaKos;
    Button btnSimpan;
    ImageButton btnBack;
    SessionManager sessionManager;
    String emailLama;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // Pastikan XML tadi namanya benar

        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.et_profile_email);
        etNamaKos = findViewById(R.id.et_profile_nama_kos);
        btnSimpan = findViewById(R.id.btn_simpan_profile);
        btnBack = findViewById(R.id.btn_back);

        // Load Data Awal
        emailLama = sessionManager.getEmail();
        etEmail.setText(emailLama);
        etNamaKos.setText(sessionManager.getNamaKos());

        btnBack.setOnClickListener(v -> finish());

        btnSimpan.setOnClickListener(v -> simpanProfile());
    }

    private void simpanProfile() {
        String emailBaru = etEmail.getText().toString().trim();
        String namaKosBaru = etNamaKos.getText().toString().trim();

        if (emailBaru.isEmpty() || namaKosBaru.isEmpty()) {
            Toast.makeText(this, "Data tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.updateProfile(emailLama, emailBaru, namaKosBaru).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    if (json.getString("status").equals("sukses")) {
                        Toast.makeText(ProfileActivity.this, "Profil Berhasil Diupdate!", Toast.LENGTH_SHORT).show();

                        // Update Session di HP
                        sessionManager.createLoginSession(emailBaru, namaKosBaru);
                        emailLama = emailBaru;
                    } else {
                        Toast.makeText(ProfileActivity.this, "Gagal: " + json.getString("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Error Koneksi", Toast.LENGTH_SHORT).show();
            }
        });
    }
}