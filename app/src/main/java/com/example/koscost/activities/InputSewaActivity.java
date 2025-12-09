package com.example.koscost.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.utils.CurrencyTextWatcher;
import com.example.koscost.utils.SessionManager;
import com.example.koscost.database.DatabaseHelper; // Import Database Helper

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Calendar;
import java.util.Locale;

public class InputSewaActivity extends AppCompatActivity {

    EditText etNama, etWa, etPekerjaan, etTglIn, etTglOut, etTotal, etBayar, etMetode;
    TextView tvNoKamar;
    Button btnSimpan;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_sewa);

        // Inisialisasi Session
        sessionManager = new SessionManager(this);

        // Binding View
        etNama = findViewById(R.id.et_nama_penghuni);
        etWa = findViewById(R.id.et_nomor_wa);
        etPekerjaan = findViewById(R.id.et_pekerjaan);
        tvNoKamar = findViewById(R.id.et_nomor_kamar);
        etTglIn = findViewById(R.id.et_tgl_checkin);
        etTglOut = findViewById(R.id.et_tgl_checkout);
        etTotal = findViewById(R.id.et_total_harga);
        etBayar = findViewById(R.id.et_jumlah_bayar);
        etMetode = findViewById(R.id.et_metode_bayar);
        btnSimpan = findViewById(R.id.btn_simpan);

        // Ambil Data dari Intent (Nomor Kamar)
        String noKamar = getIntent().getStringExtra("NO_KAMAR");
        if (noKamar != null) {
            tvNoKamar.setText(noKamar);
        }

        // Auto-fill harga jika ada dari intent (Opsional, biar enak)
        double hargaBulanan = getIntent().getDoubleExtra("HARGA_BULANAN", 0);
        if (hargaBulanan > 0) {
            // Format manual atau biarkan kosong biar diisi user
        }

        // Pasang Format Uang (CurrencyWatcher)
        etTotal.addTextChangedListener(new CurrencyTextWatcher(etTotal));
        etBayar.addTextChangedListener(new CurrencyTextWatcher(etBayar));

        // Pasang Picker Tanggal
        setupDatePicker(etTglIn);
        setupDatePicker(etTglOut);

        btnSimpan.setOnClickListener(v -> simpanData());
    }

    private void setupDatePicker(EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String selectedDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
                editText.setText(selectedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private String hitungDurasiOtomatis(String tglMasuk, String tglKeluar) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.util.Date dateIn = sdf.parse(tglMasuk);
            java.util.Date dateOut = sdf.parse(tglKeluar);

            long diff = dateOut.getTime() - dateIn.getTime();
            long days = java.util.concurrent.TimeUnit.DAYS.convert(diff, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (days < 7) {
                return "Harian (" + days + " Hari)";
            } else if (days <= 13) {
                return "Mingguan";
            } else {
                return "Bulanan";
            }
        } catch (Exception e) {
            return "Bulanan"; // Default kalau error parsing
        }
    }

    private void simpanData() {
        String noKamar = tvNoKamar.getText().toString();
        String nama = etNama.getText().toString();
        String wa = etWa.getText().toString();
        String kerja = etPekerjaan.getText().toString();
        String tglIn = etTglIn.getText().toString();
        String tglOut = etTglOut.getText().toString();
        String strTotal = etTotal.getText().toString();
        String strBayar = etBayar.getText().toString();
        String metode = etMetode.getText().toString();

        if (nama.isEmpty() || strTotal.isEmpty() || tglIn.isEmpty() || tglOut.isEmpty()) {
            Toast.makeText(this, "Data wajib diisi lengkap!", Toast.LENGTH_SHORT).show();
            return;
        }

        String emailUser = sessionManager.getEmail();
        if (emailUser == null) {
            Toast.makeText(this, "Sesi Habis, Login Ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalHarga = CurrencyTextWatcher.parseCurrency(strTotal);
        double uangBayar = CurrencyTextWatcher.parseCurrency(strBayar);
        String statusLunas = (uangBayar >= totalHarga) ? "Lunas" : "Belum Lunas";

        // Hitung Durasi Otomatis
        String durasiOtomatis = hitungDurasiOtomatis(tglIn, tglOut);
        String periodeLengkap = tglIn + " s.d " + tglOut + " (" + durasiOtomatis + ")";

        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.simpanSewa(
                emailUser,
                noKamar, nama, wa, kerja,
                durasiOtomatis,
                totalHarga, statusLunas
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InputSewaActivity.this, "Check-In Berhasil (Online)!", Toast.LENGTH_SHORT).show();
                    pindahKeKuitansi(nama, noKamar, periodeLengkap, totalHarga, statusLunas, metode);
                } else {
                    // Jika server error (misal 500), simpan offline juga
                    simpanOfflineDanLanjut(emailUser, noKamar, nama, wa, kerja, durasiOtomatis, totalHarga, statusLunas, periodeLengkap, metode, tglIn, tglOut);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // JIKA GAGAL KONEKSI -> SIMPAN LOKAL (OFFLINE MODE)
                simpanOfflineDanLanjut(emailUser, noKamar, nama, wa, kerja, durasiOtomatis, totalHarga, statusLunas, periodeLengkap, metode, tglIn, tglOut);
            }
        });
    }

    // Method bantuan untuk menyimpan ke SQLite saat offline
    // Method bantuan untuk menyimpan ke SQLite saat offline
    private void simpanOfflineDanLanjut(String email, String no, String nama, String wa, String kerja, String durasi, double harga, String status, String periodeDisplay, String metode, String tglIn, String tglOut) {
        DatabaseHelper db = new DatabaseHelper(InputSewaActivity.this);

        // Panggil method addPendingSewa yang BARU (ada parameter tanggalnya)
        db.addPendingSewa(email, no, nama, wa, kerja, durasi, harga, status, tglIn, tglOut);

        Toast.makeText(InputSewaActivity.this, "Offline: Data Tersimpan di HP.", Toast.LENGTH_LONG).show();

        // Tetap lanjut ke kuitansi
        pindahKeKuitansi(nama, no, periodeDisplay, harga, status, metode);
    }

    // Method bantuan untuk pindah intent (biar tidak duplikat kode)
    private void pindahKeKuitansi(String nama, String kamar, String periode, double harga, String status, String metode) {
        Intent intent = new Intent(InputSewaActivity.this, CetakKuitansiActivity.class);
        intent.putExtra("NAMA", nama);
        intent.putExtra("KAMAR", kamar);
        intent.putExtra("PERIODE", periode);
        intent.putExtra("HARGA", harga);
        intent.putExtra("STATUS", status);
        intent.putExtra("METODE", metode);
        startActivity(intent);
        finish();
    }
}