package com.example.koscost.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.utils.SessionManager;
import com.example.koscost.utils.CurrencyTextWatcher; // Import ini

import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TambahKamarActivity extends AppCompatActivity {

    EditText etNo, etFasilitas, etHarian, etMingguan, etBulanan;
    Button btnSimpan;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tambah_kamar);

        sessionManager = new SessionManager(this);

        etNo = findViewById(R.id.et_no_kamar_baru);
        etFasilitas = findViewById(R.id.et_fasilitas_baru);
        etHarian = findViewById(R.id.et_harga_harian);
        etMingguan = findViewById(R.id.et_harga_mingguan);
        etBulanan = findViewById(R.id.et_harga_bulanan);
        btnSimpan = findViewById(R.id.btn_simpan_kamar);

        // --- PASANG FORMAT UANG (TITIK) ---
        etHarian.addTextChangedListener(new CurrencyTextWatcher(etHarian));
        etMingguan.addTextChangedListener(new CurrencyTextWatcher(etMingguan));
        etBulanan.addTextChangedListener(new CurrencyTextWatcher(etBulanan));

        btnSimpan.setOnClickListener(v -> simpanKamar());
    }

    private void simpanKamar() {
        String no = etNo.getText().toString().trim();
        String fas = etFasilitas.getText().toString().trim();

        // Ambil text mentah (masih ada titiknya)
        String strHarian = etHarian.getText().toString();
        String strMingguan = etMingguan.getText().toString();
        String strBulanan = etBulanan.getText().toString();

        if (no.isEmpty() || strBulanan.isEmpty()) {
            Toast.makeText(this, "Nomor & Harga Bulanan Wajib Diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        String emailUser = sessionManager.getEmail();
        if (emailUser == null) {
            Toast.makeText(this, "Sesi habis, login ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- BERSIHKAN TITIK SEBELUM KIRIM KE SERVER ---
        double harian = strHarian.isEmpty() ? 0 : CurrencyTextWatcher.parseCurrency(strHarian);
        double mingguan = strMingguan.isEmpty() ? 0 : CurrencyTextWatcher.parseCurrency(strMingguan);
        double bulanan = CurrencyTextWatcher.parseCurrency(strBulanan);

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.tambahKamar(emailUser, no, fas, harian, mingguan, bulanan).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);
                        if (json.getString("status").equals("sukses")) {
                            Toast.makeText(TambahKamarActivity.this, "Kamar Berhasil Ditambah!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(TambahKamarActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(TambahKamarActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}