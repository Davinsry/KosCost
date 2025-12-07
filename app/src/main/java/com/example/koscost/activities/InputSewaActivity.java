package com.example.koscost.activities; // Sesuaikan jika nama package beda

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.koscost.R;
import com.example.koscost.database.DatabaseHelper;

import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InputSewaActivity extends AppCompatActivity {

    // 1. Deklarasi Variabel
    EditText etNama, etWa, etPekerjaan, etNoKamar, etTglIn, etTglOut, etTotal, etBayar, etMetode;
    Button btnSimpan;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_sewa);

        // 2. Inisialisasi Database
        dbHelper = new DatabaseHelper(this);

        // 3. Binding (Sambungkan variabel dengan ID di XML)
        etNama = findViewById(R.id.et_nama_penghuni);
        etWa = findViewById(R.id.et_nomor_wa);
        etPekerjaan = findViewById(R.id.et_pekerjaan);
        etNoKamar = findViewById(R.id.et_nomor_kamar);
        etTglIn = findViewById(R.id.et_tgl_checkin);
        etTglOut = findViewById(R.id.et_tgl_checkout);
        etTotal = findViewById(R.id.et_total_harga);
        etBayar = findViewById(R.id.et_jumlah_bayar);
        etMetode = findViewById(R.id.et_metode_bayar);
        btnSimpan = findViewById(R.id.btn_simpan);

        // 4. Aksi saat Tombol Simpan Ditekan
        btnSimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simpanData();
            }
        });
    }

    private void simpanData() {
        // 1. Ambil data string seperti biasa
        String nama = etNama.getText().toString();
        String wa = etWa.getText().toString();
        String kerja = etPekerjaan.getText().toString();
        String noKamar = etNoKamar.getText().toString();
        // ... (ambil data lainnya seperti tgl dll, sesuaikan variabelmu)
        String strTotal = etTotal.getText().toString();

        if (nama.isEmpty() || noKamar.isEmpty()) {
            Toast.makeText(this, "Data tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalHarga = Double.parseDouble(strTotal);
        String statusLunas = "Lunas"; // Logika sederhana dulu

        // 2. KIRIM KE VPS PAKAI RETROFIT
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.simpanSewa(
                noKamar, nama, wa, kerja, "Bulanan", totalHarga, statusLunas
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InputSewaActivity.this, "Berhasil Disimpan di Server!", Toast.LENGTH_SHORT).show();

                    // 3. PINDAH KE KUITANSI (BAWA DATA LANGSUNG)
                    // Kita "lempar" data yang barusan diketik ke halaman sebelah
                    Intent intent = new Intent(InputSewaActivity.this, CetakKuitansiActivity.class);
                    intent.putExtra("NAMA", nama);
                    intent.putExtra("KAMAR", noKamar);
                    intent.putExtra("HARGA", totalHarga);
                    intent.putExtra("STATUS", statusLunas);
                    intent.putExtra("TGL", "Hari Ini"); // Atau ambil dari EditText
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(InputSewaActivity.this, "Gagal respon server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(InputSewaActivity.this, "Error Koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}