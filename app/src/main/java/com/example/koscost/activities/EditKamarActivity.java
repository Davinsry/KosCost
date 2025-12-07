package com.example.koscost.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

public class EditKamarActivity extends AppCompatActivity {

    EditText etNo, etFasilitas, etHarian, etMingguan, etBulanan;
    Button btnUpdate;
    String idKamar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_kamar);

        // Inisialisasi View
        etNo = findViewById(R.id.et_no_kamar_baru);
        etFasilitas = findViewById(R.id.et_fasilitas_baru);
        etHarian = findViewById(R.id.et_harga_harian);
        etMingguan = findViewById(R.id.et_harga_mingguan);
        etBulanan = findViewById(R.id.et_harga_bulanan);

        // PERBAIKAN DI SINI: Gunakan ID yang sesuai dengan activity_edit_kamar.xml
        btnUpdate = findViewById(R.id.btn_update_kamar);

        // 1. Tangkap Data dari Intent (Dikirim dari Dashboard)
        // Pastikan cek null atau default value agar aman
        if (getIntent() != null) {
            idKamar = getIntent().getStringExtra("ID");
            etNo.setText(getIntent().getStringExtra("NO"));
            etFasilitas.setText(getIntent().getStringExtra("FAS"));
            etHarian.setText(String.valueOf(getIntent().getDoubleExtra("H_HARIAN", 0)));
            etMingguan.setText(String.valueOf(getIntent().getDoubleExtra("H_MINGGUAN", 0)));
            etBulanan.setText(String.valueOf(getIntent().getDoubleExtra("H_BULANAN", 0)));
        }

        // 2. Aksi Tombol Update
        // Tidak perlu setText lagi karena di XML textnya sudah "UPDATE DATA"
        btnUpdate.setOnClickListener(v -> updateKamar());
    }

    private void updateKamar() {
        // Cek validasi input kosong agar tidak crash saat parseDouble
        if (etHarian.getText().toString().isEmpty() || etMingguan.getText().toString().isEmpty() || etBulanan.getText().toString().isEmpty()) {
            Toast.makeText(this, "Harga tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        String no = etNo.getText().toString();
        String fas = etFasilitas.getText().toString();
        double harian = Double.parseDouble(etHarian.getText().toString());
        double mingguan = Double.parseDouble(etMingguan.getText().toString());
        double bulanan = Double.parseDouble(etBulanan.getText().toString());

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.editKamar(idKamar, no, fas, harian, mingguan, bulanan).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    Toast.makeText(EditKamarActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();

                    if (json.getString("status").equals("sukses")) {
                        finish(); // Tutup halaman dan kembali ke Dashboard
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(EditKamarActivity.this, "Terjadi kesalahan respon", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(EditKamarActivity.this, "Gagal Update: Koneksi Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}