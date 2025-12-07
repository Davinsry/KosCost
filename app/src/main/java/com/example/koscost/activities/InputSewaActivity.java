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
import com.example.koscost.utils.CurrencyTextWatcher; // Pastikan ini ada
import com.example.koscost.utils.SessionManager; // Tambah SessionManager

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
    SessionManager sessionManager; // Variabel Session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_sewa);

        // Inisialisasi Session
        sessionManager = new SessionManager(this);

        // Binding
        etNama = findViewById(R.id.et_nama_penghuni);
        etWa = findViewById(R.id.et_nomor_wa);
        etPekerjaan = findViewById(R.id.et_pekerjaan);
        tvNoKamar = findViewById(R.id.et_nomor_kamar); // Pastikan di XML pakai TextView
        etTglIn = findViewById(R.id.et_tgl_checkin);
        etTglOut = findViewById(R.id.et_tgl_checkout);
        etTotal = findViewById(R.id.et_total_harga);
        etBayar = findViewById(R.id.et_jumlah_bayar);
        etMetode = findViewById(R.id.et_metode_bayar);
        btnSimpan = findViewById(R.id.btn_simpan);

        // Ambil Nomor Kamar
        String noKamar = getIntent().getStringExtra("NO_KAMAR");
        if (noKamar != null) {
            tvNoKamar.setText(noKamar);
        }

        // Pasang Format Uang
        etTotal.addTextChangedListener(new CurrencyTextWatcher(etTotal));
        etBayar.addTextChangedListener(new CurrencyTextWatcher(etBayar));

        // Pasang Kalendar
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
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
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
        if (emailUser == null) return;

        double totalHarga = CurrencyTextWatcher.parseCurrency(strTotal);
        double uangBayar = CurrencyTextWatcher.parseCurrency(strBayar);
        String statusLunas = (uangBayar >= totalHarga) ? "Lunas" : "Belum Lunas";

        // --- HITUNG DURASI OTOMATIS DI SINI ---
        String durasiOtomatis = hitungDurasiOtomatis(tglIn, tglOut);

        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.simpanSewa(
                emailUser,
                noKamar, nama, wa, kerja,
                durasiOtomatis, // Pakai hasil hitungan
                totalHarga, statusLunas
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InputSewaActivity.this, "Check-In Berhasil!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(InputSewaActivity.this, CetakKuitansiActivity.class);
                    intent.putExtra("NAMA", nama);
                    intent.putExtra("KAMAR", noKamar);
                    intent.putExtra("PERIODE", tglIn + " s.d " + tglOut + " (" + durasiOtomatis + ")");
                    intent.putExtra("HARGA", totalHarga);
                    intent.putExtra("STATUS", statusLunas);
                    intent.putExtra("METODE", metode);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(InputSewaActivity.this, "Gagal Server", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(InputSewaActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}