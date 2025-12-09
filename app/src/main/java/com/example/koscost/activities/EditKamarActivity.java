package com.example.koscost.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.database.DatabaseHelper; // Import DatabaseHelper
import com.example.koscost.utils.CurrencyTextWatcher;

import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditKamarActivity extends AppCompatActivity {

    EditText etNo, etFasilitas, etHarian, etMingguan, etBulanan;
    Button btnUpdate;
    ImageButton btnBack;
    TextView tvJudul;
    String idKamar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_kamar);

        // Binding View
        etNo = findViewById(R.id.et_no_kamar_baru);
        etFasilitas = findViewById(R.id.et_fasilitas_baru);
        etHarian = findViewById(R.id.et_harga_harian);
        etMingguan = findViewById(R.id.et_harga_mingguan);
        etBulanan = findViewById(R.id.et_harga_bulanan);
        btnUpdate = findViewById(R.id.btn_update_kamar); // Pastikan ID ini sesuai XML
        btnBack = findViewById(R.id.btn_back);
        tvJudul = findViewById(R.id.tv_judul_tambah); // ID judul di XML mungkin perlu disesuaikan jika beda

        // Setup Back Button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        if (tvJudul != null) {
            tvJudul.setText("Edit Data Kamar");
        }

        // Pasang Currency Formatter
        etHarian.addTextChangedListener(new CurrencyTextWatcher(etHarian));
        etMingguan.addTextChangedListener(new CurrencyTextWatcher(etMingguan));
        etBulanan.addTextChangedListener(new CurrencyTextWatcher(etBulanan));

        // Tangkap Data Intent
        if (getIntent() != null) {
            idKamar = getIntent().getStringExtra("ID");
            etNo.setText(getIntent().getStringExtra("NO"));
            etFasilitas.setText(getIntent().getStringExtra("FAS"));

            // Set harga (format double ke string integer tanpa koma desimal .0)
            etHarian.setText(String.format("%.0f", getIntent().getDoubleExtra("H_HARIAN", 0)));
            etMingguan.setText(String.format("%.0f", getIntent().getDoubleExtra("H_MINGGUAN", 0)));
            etBulanan.setText(String.format("%.0f", getIntent().getDoubleExtra("H_BULANAN", 0)));
        }

        btnUpdate.setOnClickListener(v -> updateKamar());
    }

    private void updateKamar() {
        // Validasi Input
        if (etNo.getText().toString().isEmpty() || etBulanan.getText().toString().isEmpty()) {
            Toast.makeText(this, "Nomor Kamar & Harga Bulanan Wajib Diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        String no = etNo.getText().toString();
        String fas = etFasilitas.getText().toString();

        // Ambil value murni dari CurrencyWatcher
        double harian = CurrencyTextWatcher.parseCurrency(etHarian.getText().toString());
        double mingguan = CurrencyTextWatcher.parseCurrency(etMingguan.getText().toString());
        double bulanan = CurrencyTextWatcher.parseCurrency(etBulanan.getText().toString());

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.editKamar(idKamar, no, fas, harian, mingguan, bulanan).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        if (json.getString("status").equals("sukses")) {
                            Toast.makeText(EditKamarActivity.this, "Kamar Berhasil Diedit!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            // Gagal dari server, tapi bukan error koneksi -> Cek pesan error
                            Toast.makeText(EditKamarActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Error server (500, 404) -> Simpan Offline
                        editOffline(idKamar, no, fas, harian, mingguan, bulanan);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Error parsing -> Simpan Offline
                    editOffline(idKamar, no, fas, harian, mingguan, bulanan);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Gagal Koneksi -> Simpan Offline
                editOffline(idKamar, no, fas, harian, mingguan, bulanan);
            }
        });
    }

    private void editOffline(String id, String no, String fas, double h, double m, double b) {
        DatabaseHelper db = new DatabaseHelper(this);
        db.addPendingEditKamar(id, no, fas, h, m, b);
        Toast.makeText(this, "Offline: Perubahan Kamar Disimpan.", Toast.LENGTH_SHORT).show();
        finish();
    }
}