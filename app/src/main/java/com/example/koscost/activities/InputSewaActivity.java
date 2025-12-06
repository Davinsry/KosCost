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
        // A. Ambil teks dari inputan
        String nama = etNama.getText().toString();
        String wa = etWa.getText().toString();
        String kerja = etPekerjaan.getText().toString();
        String noKamar = etNoKamar.getText().toString();
        String tglIn = etTglIn.getText().toString();
        String tglOut = etTglOut.getText().toString();
        String strTotal = etTotal.getText().toString();
        String strBayar = etBayar.getText().toString();
        String metode = etMetode.getText().toString();

        // B. Validasi (Cek biar gak kosong)
        if (nama.isEmpty() || noKamar.isEmpty() || strTotal.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi data wajib (Nama, Kamar, Harga)!", Toast.LENGTH_SHORT).show();
            return;
        }

        // C. Konversi Harga ke Angka (Double)
        double totalHarga = Double.parseDouble(strTotal);
        double uangBayar = Double.parseDouble(strBayar);

        // D. Tentukan Status Lunas/Belum
        String statusLunas;
        if (uangBayar >= totalHarga) {
            statusLunas = "Lunas";
        } else {
            statusLunas = "Belum Lunas";
        }

        // --- LOGIKA DATABASE ---
        // Karena ini prototype TA, kita buat simpel:
        // 1. Kita anggap setiap input kamar baru itu membuat data kamar baru juga di database master
        // Agar kita dapat ID Kamarnya.

        dbHelper.addKamar(noKamar, "Standar", totalHarga);
        // (Di aplikasi nyata, harusnya cek dulu kamarnya sudah ada atau belum, tapi ini shortcut biar cepat)

        // 2. Karena SQLite autoincrement, kita perlu ID kamar terakhir yang barusan dibuat.
        // Tapi untuk simplifikasi TA, kita bisa tembak ID manual atau ambil query terakhir.
        // SEMENTARA: Kita pakai ID dummy atau logika pencarian sederhana.
        // (Agar tidak error, kita asumsi ID Kamar = 1 atau kita query dulu.
        // TAPI, biar gampang di tahap ini, kita masukkan ID Kamar = Integer dari No Kamar (misal kamar 203 -> id 203)

        int idKamarInt = 0;
        try {
            idKamarInt = Integer.parseInt(noKamar);
        } catch (NumberFormatException e) {
            idKamarInt = 1; // Default kalau user masukin "Kamar Mawar"
        }

        // 3. Simpan ke Tabel Sewa (Check-In)
        boolean isSuccess = dbHelper.prosesCheckIn(
                idKamarInt, // ID Kamar
                nama,
                wa,
                "-", // KTP (skip dulu)
                kerja,
                tglIn,
                tglOut,
                "Bulanan", // Tipe Sewa default
                totalHarga,
                uangBayar,
                metode,
                statusLunas
        );

        if (isSuccess) {
            Toast.makeText(this, "Check-In Berhasil!", Toast.LENGTH_SHORT).show();

            // --- NAVIGATION ---
            // Pindah ke Halaman Kuitansi sambil bawa ID Sewanya
            // Tapi tunggu, method prosesCheckIn kita cuma return True/False, tidak return ID Sewa.
            // Biar gampang, kita query ambil ID terakhir dari tabel sewa.

            // Note: Idealnya method insert return ID, tapi gapapa kita akali di Intent
            // Kita akan modifikasi sedikit nanti di helper, tapi untuk sekarang
            // kita arahkan user ke halaman kuitansi dengan data statis dulu untuk tes layout
            // ATAU kita cari ID terakhir (max id).

            int lastId = dbHelper.getLastIdSewa(); // <--- Kita perlu tambah fungsi kecil ini di DatabaseHelper sebentar

            Intent intent = new Intent(InputSewaActivity.this, CetakKuitansiActivity.class);
            intent.putExtra("ID_SEWA", lastId);
            startActivity(intent);
            finish(); // Tutup halaman input biar gak bisa back

        } else {
            Toast.makeText(this, "Gagal Simpan Data", Toast.LENGTH_SHORT).show();
        }
    }
}